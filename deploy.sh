#!/bin/bash

# Watchtower 기반 자동 배포 스크립트
# 한 번만 실행하면 이후 모든 배포가 자동화됨

set -e

echo "🐳 Watchtower 기반 자동 배포 시작..."

# 현재 디렉토리 확인
echo "📂 현재 디렉토리: $(pwd)"

# 필수 파일 확인
if [ ! -f "docker-compose.yml" ]; then
    echo "❌ docker-compose.yml 파일이 없습니다!"
    exit 1
fi

# Docker 및 Docker Compose 설치 확인
if ! command -v docker &> /dev/null; then
    echo "🔧 Docker 설치 중..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    echo "✅ Docker 설치 완료"
fi

if ! command -v docker-compose &> /dev/null; then
    echo "🔧 Docker Compose 설치 중..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose 설치 완료"
fi

# .env 파일 확인/생성
if [ ! -f ".env" ]; then
    echo "📝 .env 파일 생성 중..."
    cat > .env << EOF
NCP_REGISTRY_URL=여기에_REGISTRY_URL_입력
EOF
    echo "⚠️  .env 파일을 편집하여 실제 Container Registry URL을 입력해주세요!"
    echo "   nano .env"
    exit 1
fi

# Container Registry 로그인 확인
echo "🔐 Container Registry 로그인 확인..."
if [ -f "/home/ubuntu/.docker/config.json" ]; then
    echo "✅ Docker 인증 설정이 존재합니다"
else
    echo "⚠️  Container Registry 로그인이 필요합니다:"
    echo "   docker login [REGISTRY_URL] -u [ACCESS_KEY] -p [SECRET_KEY]"
    echo "   로그인 후 다시 실행해주세요"
    exit 1
fi

# 기존 컨테이너 중지 (있는 경우)
echo "⏹️  기존 컨테이너 중지 중..."
docker-compose down || true

# 새로운 구성으로 시작
echo "🚀 Watchtower 기반 서비스 시작..."
docker-compose up -d

# 상태 확인
echo "🔍 서비스 상태 확인 중..."
sleep 10

if docker-compose ps | grep -q "Up"; then
    echo "✅ 서비스가 성공적으로 시작되었습니다!"
    echo ""
    echo "📊 실행 중인 서비스:"
    docker-compose ps
    echo ""
    echo "🎉 Watchtower 자동 배포 설정 완료!"
    echo ""
    echo "📋 이제 다음이 자동으로 실행됩니다:"
    echo "   ✅ 5분마다 Container Registry에서 새 이미지 체크"
    echo "   ✅ 새 이미지 발견 시 자동 다운로드 및 배포"
    echo "   ✅ 이전 이미지 자동 정리"
    echo "   ✅ Health check 및 자동 재시작"
    echo ""
    echo "🔍 로그 확인:"
    echo "   docker-compose logs -f app      # 애플리케이션 로그"
    echo "   docker-compose logs -f watchtower # Watchtower 로그"
else
    echo "❌ 서비스 시작에 실패했습니다"
    echo "📋 로그 확인:"
    docker-compose logs
    exit 1
fi 
