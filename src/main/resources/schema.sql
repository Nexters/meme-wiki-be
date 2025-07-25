-- 해시태그 테이블
create table hashtag
(
    id         bigint auto_increment primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    name       varchar(255) not null unique
);

-- 밈 테이블
create table meme
(
    id          bigint auto_increment primary key,
    created_at  timestamp(6),
    updated_at  timestamp(6),
    title       varchar(255) not null comment '밈 제목',
    description varchar(255),
    category    enum('NONE') not null comment '밈 분류 카테고리',
    img_url     varchar(255) null comment '이미지 url',
);

-- 밈 해시태그 관계 테이블
create table meme_hashtag
(
    id         bigint auto_increment primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    meme_id    bigint not null,
    hashtag_id bigint not null,
);

-- 밈 조회 로그 테이블
create table meme_view_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    meme_id    bigint not null,
);

-- 밈 커스텀(나만의 밈 만들기) 로그 테이블
create table meme_custom_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    meme_id    bigint not null,
);

-- 밈 공유 로그 테이블
create table meme_share_log
(
    id         bigint auto_increment primary key,
    created_at timestamp(6),
    updated_at timestamp(6),
    meme_id    bigint not null,
);
