# GCP 배포 세팅 가이드

NCP에서 GCP 프리티어로 마이그레이션하기 위한 전체 세팅 과정입니다.

## 전제 조건

- Google 계정
- `gcloud` CLI 설치 ([설치 가이드](https://cloud.google.com/sdk/docs/install))
- GitHub 레포지토리 Settings > Secrets 접근 권한

---

## 1. GCP 프로젝트 생성

```bash
# gcloud 로그인
gcloud auth login

# 프로젝트 생성 (프로젝트 ID는 글로벌 고유해야 함, 중복 시 숫자 접미사 변경)
gcloud projects create meme-wiki-be-2026 --name="Meme Wiki"

# 생성한 프로젝트를 기본으로 설정
gcloud config set project meme-wiki-be-2026
```

결제 계정 연결:
```bash
gcloud billing projects link meme-wiki-be-2026 \
  --billing-account=01C9BF-C39CDA-222910
```
또는 [Google Cloud Console](https://console.cloud.google.com/billing) > 결제 계정 > 내 프로젝트 > 프로젝트 연결. 프리티어도 결제 계정이 필요합니다 (무료 한도 내에서는 과금되지 않음).

---

## 2. 필요한 API 활성화

```bash
gcloud services enable \
  artifactregistry.googleapis.com \
  compute.googleapis.com
```

---

## 3. Artifact Registry (Docker 이미지 저장소) 생성

```bash
gcloud artifacts repositories create meme-wiki \
  --repository-format=docker \
  --location=us-central1 \
  --description="meme-wiki Docker images"
```

생성된 레지스트리 URL: `us-central1-docker.pkg.dev/meme-wiki-be-2026/meme-wiki`

확인:
```bash
gcloud artifacts repositories list
```

---

## 4. CI/CD용 서비스 계정 생성

GitHub Actions가 Artifact Registry에 이미지를 push할 때 사용할 서비스 계정입니다.

```bash
# 서비스 계정 생성
gcloud iam service-accounts create github-cicd \
  --display-name="GitHub CI/CD"

# Artifact Registry Writer 권한 부여
gcloud projects add-iam-policy-binding meme-wiki-be-2026 \
  --member="serviceAccount:github-cicd@meme-wiki-be-2026.iam.gserviceaccount.com" \
  --role="roles/artifactregistry.writer"

# JSON 키 파일 다운로드 (이 파일 내용을 GitHub Secret에 등록)
gcloud iam service-accounts keys create github-cicd-key.json \
  --iam-account=github-cicd@meme-wiki-be-2026.iam.gserviceaccount.com
```

> `github-cicd-key.json` 파일의 전체 내용이 나중에 GitHub Secret `GCP_SA_KEY_JSON`에 들어갑니다.

---

## 5. Compute Engine VM 생성 (프리티어)

```bash
gcloud compute instances create meme-wiki-server \
  --zone=us-central1-a \
  --machine-type=e2-micro \
  --image-family=ubuntu-2404-lts-amd64 \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=30GB \
  --tags=http-server,https-server
```

### 프리티어 조건
- **e2-micro** 인스턴스만 무료
- **us-west1, us-central1, us-east1** 리전만 무료
- **30GB** standard persistent disk까지 무료

---

## 6. 고정 외부 IP 할당

VM을 재시작해도 IP가 바뀌지 않도록 고정 IP를 할당합니다.

```bash
# 고정 IP 생성
gcloud compute addresses create meme-wiki-ip --region=us-central1

# 할당된 IP 확인
gcloud compute addresses describe meme-wiki-ip --region=us-central1 --format='get(address)'

# 기존 임시 IP 제거 후 고정 IP 연결
gcloud compute instances delete-access-config meme-wiki-server \
  --zone=us-central1-a \
  --access-config-name="external-nat"

gcloud compute instances add-access-config meme-wiki-server \
  --zone=us-central1-a \
  --address=34.60.207.191
```

> 고정 외부 IP는 VM에 연결되어 있으면 무료, 연결 해제 상태면 과금됩니다.

---

## 7. 방화벽 규칙 설정

```bash
gcloud compute firewall-rules create allow-meme-wiki \
  --allow=tcp:80,tcp:443,tcp:8080 \
  --target-tags=http-server,https-server \
  --description="Allow HTTP, HTTPS, and app traffic"
```

---

## 8. VM 초기 설정

### 8-1. SSH 접속

```bash
gcloud compute ssh meme-wiki-server --zone=us-central1-a
```

### 8-2. Docker 설치

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER

# 재접속하여 docker 그룹 적용
exit
gcloud compute ssh meme-wiki-server --zone=us-central1-a
```

### 8-3. Swap 설정 (e2-micro 메모리 부족 대비)

e2-micro는 1GB RAM이라 Spring Boot + MySQL을 돌리면 메모리가 부족할 수 있습니다. 2GB swap을 추가합니다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# 재부팅 후에도 유지되도록 설정
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 확인
free -h
```

### 8-4. 디렉토리 구조 생성

```bash
sudo mkdir -p /home/ubuntu/app
sudo mkdir -p /root/prod/logs
sudo mkdir -p /root/config
```

### 8-5. GCP Artifact Registry Docker 인증

```bash
# gcloud CLI 설치 (Ubuntu에 없는 경우)
sudo apt-get install -y apt-transport-https ca-certificates gnupg curl
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg
echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list
sudo apt-get update && sudo apt-get install -y google-cloud-cli

# Docker 인증 설정
sudo gcloud auth configure-docker us-central1-docker.pkg.dev
```

이 명령이 `/root/.docker/config.json`에 인증 정보를 저장합니다. Watchtower가 이 파일을 마운트(`/root/.docker/config.json:/config.json:ro`)하므로 자동으로 이미지를 pull할 수 있게 됩니다.

### 8-6. SSH 키 설정 (GitHub Actions 배포용)

```bash
# SSH 키페어 생성 (로컬 머신에서)
ssh-keygen -t ed25519 -C "github-actions" -f ~/.ssh/gcp_deploy_key

# 공개키를 VM에 추가 (VM에서)
echo "<공개키 내용>" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
```

> `gcp_deploy_key`(비밀키)의 내용이 GitHub Secret `SSH_PRIVATE_KEY`에 들어갑니다.

---

## 9. GitHub Secrets 설정

GitHub 레포지토리 > Settings > Secrets and variables > Actions 에서 설정합니다.

### 삭제할 Secrets (NCP 관련)

| Secret | 이유 |
|--------|------|
| `NCP_REGISTRY_URL` | GCP로 대체 |
| `NCP_ACCESS_KEY` | 더 이상 불필요 |
| `NCP_SECRET_KEY` | 더 이상 불필요 |
| `NCP_SERVER_HOST` | GCP로 대체 |
| `MYSQL_DEV_DATABASE` | dev 환경 제거 |

### 추가할 Secrets

| Secret | 값 | 설명 |
|--------|-----|------|
| `GCP_SA_KEY_JSON` | `github-cicd-key.json` 파일 내용 전체 | Artifact Registry 인증 |
| `GCP_SERVER_HOST` | `34.60.207.191` | SSH 배포 대상 |
| `GCP_REGISTRY_URL` | `us-central1-docker.pkg.dev/meme-wiki-be-2026/meme-wiki` | Docker 레지스트리 URL |

### 유지하는 Secrets (값은 동일, 그대로 유지)

| Secret | 용도 |
|--------|------|
| `SSH_PRIVATE_KEY` | SSH 배포 (새 키페어 사용 시 값 업데이트) |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `MYSQL_DATABASE` | prod DB 이름 |
| `MYSQL_USER` | MySQL 사용자 |
| `MYSQL_PASSWORD` | MySQL 비밀번호 |
| `CLOUDFLARE_R2_ACCESS_KEY_ID` | R2 이미지 저장소 |
| `CLOUDFLARE_R2_SECRET_ACCESS_KEY` | R2 인증 |
| `CLOUDFLARE_R2_ENDPOINT` | R2 엔드포인트 |
| `CLOUDFLARE_R2_BUCKET_NAME` | R2 버킷 이름 |
| `ADMIN_USERNAME` | 관리자 계정 |
| `ADMIN_PASSWORD` | 관리자 비밀번호 |
| `GOOGLE_GENAI_API_KEY` | Gemini API |
| `NAVER_AI_API_KEY` | CLOVA Studio API |
| `PINECONE_API_KEY` | Pinecone 벡터 DB |
| `PINECONE_INDEX_NAME` | Pinecone 인덱스 |
| `PINECONE_ENVIRONMENT` | Pinecone 환경 |
| `FCM_SERVICE_ACCOUNT_JSON` | Firebase 푸시 알림 |

---

## 10. MySQL 데이터 마이그레이션

### NCP 서버에서 덤프

```bash
# NCP 서버에 SSH 접속 후
docker exec meme-wiki-mysql mysqldump \
  -u root -p'<MYSQL_ROOT_PASSWORD>' <MYSQL_DATABASE> > prod_dump.sql
```

### GCP VM으로 전송

```bash
scp prod_dump.sql root@34.60.207.191:/home/ubuntu/app/
```

### GCP VM에서 임포트

```bash
cd /home/ubuntu/app

# MySQL만 먼저 시작 (환경변수 필요)
export MYSQL_ROOT_PASSWORD='<비밀번호>'
export MYSQL_DATABASE='<DB이름>'
export MYSQL_USER='<사용자>'
export MYSQL_PASSWORD='<비밀번호>'
export GCP_REGISTRY_URL='<레지스트리URL>'

docker compose up -d mysql

# MySQL이 healthy 상태가 될 때까지 대기
docker compose ps  # STATUS가 healthy인지 확인

# 데이터 임포트
docker exec -i meme-wiki-mysql mysql \
  -u root -p'<MYSQL_ROOT_PASSWORD>' <MYSQL_DATABASE> < prod_dump.sql

# 확인
docker exec meme-wiki-mysql mysql \
  -u root -p'<MYSQL_ROOT_PASSWORD>' <MYSQL_DATABASE> \
  -e "SHOW TABLES;"
```

---

## 11. 첫 배포

모든 세팅이 완료되면 `master` 브랜치에 push하여 CI/CD를 트리거합니다.

```bash
git add .
git commit -m "Migrate from NCP to GCP"
git push origin master
```

GitHub Actions에서:
1. Gradle 빌드
2. Docker 이미지를 GCP Artifact Registry에 push
3. `docker-compose.yml`을 GCP VM에 SCP 전송
4. SSH로 VM에서 `docker compose pull app && docker compose up -d app`

### 이후 배포
- `master`에 push하면 자동으로 이미지가 빌드 & push됩니다
- `docker-compose.yml` 또는 `init-scripts/` 변경 시 인프라 자동 재배포
- 이미지만 변경된 경우 Watchtower가 5분 내 자동 감지 & 업데이트

---

## 12. 검증 체크리스트

```bash
# 1. 앱 헬스체크
curl http://34.60.207.191:8080/health

# 2. Watchtower 로그 확인 (이미지 pull 정상 여부)
docker logs watchtower

# 3. 앱 로그 확인
docker logs meme-wiki-be

# 4. DB 데이터 확인 (API 호출)
curl http://34.60.207.191:8080/api/memes

# 5. 이미지 업로드 테스트 (Cloudflare R2)
# 6. 추천 검색 테스트 (Naver AI + Pinecone + Vertex AI)
# 7. 푸시 알림 테스트 (FCM)
```

---

## 13. DNS 설정 - 가비아 도메인 연결

도메인이 NCP 서버 IP를 가리키고 있다면, GCP VM의 고정 IP로 변경합니다.

### Cloudflare DNS 설정 방법

네임서버가 Cloudflare(`nena.ns.cloudflare.com`, `vin.ns.cloudflare.com`)로 설정되어 있으므로, DNS는 Cloudflare에서 변경합니다.

1. [Cloudflare Dashboard](https://dash.cloudflare.com) 로그인
2. `meme-wiki.net` 도메인 선택
3. 좌측 메뉴 **DNS** > **Records** 클릭
4. 기존 NCP IP를 가리키는 A 레코드를 찾아 **Edit** 클릭하여 수정:

| 타입 | 호스트 | 값 | TTL |
|------|--------|-----|-----|
| A | @ | `34.60.207.191` | 3600 |
| A | www | `34.60.207.191` | 3600 |
| A | api (서브도메인 사용 시) | `34.60.207.191` | 3600 |

6. **확인/저장** 클릭

### 설정 확인

DNS 변경은 전파까지 최대 24시간 걸릴 수 있습니다 (보통 수 분 내 적용).

```bash
# DNS 전파 확인
nslookup your-domain.com
# 또는
dig your-domain.com +short
# 34.60.207.191 이 나오면 정상
```

### 참고
- `@`는 루트 도메인 (예: `meme-wiki.net`)
- `www`는 `www.meme-wiki.net`
- 서브도메인이 필요하면 호스트에 해당 이름 입력 (예: `api` → `api.meme-wiki.net`)

---

## 프리티어 비용 주의사항

| 항목 | 무료 한도 | 초과 시 |
|------|----------|--------|
| e2-micro VM | 월 1대 (us 리전) | 시간당 ~$0.008 |
| 디스크 | 30GB standard | GB당 ~$0.04/월 |
| 고정 IP | VM에 연결된 상태면 무료 | 미연결 시 시간당 ~$0.01 |
| Artifact Registry | 500MB 저장 | GB당 ~$0.10/월 |
| 네트워크 egress | 1GB/월 (북미 → 전체) | GB당 ~$0.12 |

> VM을 중지해도 디스크 비용은 계속 발생합니다. 고정 IP가 VM에 연결되지 않으면 과금됩니다.
