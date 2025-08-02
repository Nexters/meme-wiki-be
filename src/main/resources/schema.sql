-- 밈 테이블
create table meme
(
    id            bigint auto_increment primary key,
    created_at    timestamp(6) default current_timestamp,
    updated_at    timestamp(6) default current_timestamp,
    title         varchar(255) not null comment '밈 제목',
    origin        text null comment '밈의 유래',
    usage_context text null comment '밈 사용 맥락(언제 사용하는지 상황 맥락)',
    trend_period varchar(10) comment '밈 유행 기간 (YYYY)',
    img_url       varchar(255) null comment '이미지 url',
    hashtags      text null comment '해시태그 (json 형태로 저장)',
);

-- 밈 조회 로그 테이블
create table meme_view_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6) default current_timestamp,
    updated_at timestamp(6) default current_timestamp,
    meme_id    bigint not null,
);

-- 밈 커스텀(나만의 밈 만들기) 로그 테이블
create table meme_custom_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6) default current_timestamp,
    updated_at timestamp(6) default current_timestamp,
    meme_id    bigint not null,
);

-- 밈 공유 로그 테이블
create table meme_share_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6) default current_timestamp,
    updated_at timestamp(6) default current_timestamp,
    meme_id    bigint not null,
);

-- 카테고리
create table category
(
    id         bigint auto_increment primary key,
    created_at timestamp(6) default current_timestamp,
    updated_at timestamp(6) default current_timestamp,
    name       varchar(255) not null comment '카테고리 이름',
    img_url    varchar(255) not null comment '카테고리 대표 이미지 URL',

    constraint category_name_unique unique (name)
)

--- 밈 카테고리 관계 테이블
create table meme_category
(
    id          bigint auto_increment primary key,
    created_at timestamp(6) default current_timestamp,
    updated_at timestamp(6) default current_timestamp,
    meme_id     bigint not null,
    category_id bigint not null,
)