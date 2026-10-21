-- MySQL 운영 DB: 배포 전 한 번 적용. 개발의 ddl-auto=update로 이미 추가된 DB에는 재실행하지 않는다.
ALTER TABLE review_comment
    ADD COLUMN parent_id BIGINT NULL,
    ADD COLUMN spoiler BIT NOT NULL DEFAULT 0,
    ADD COLUMN deleted BIT NOT NULL DEFAULT 0,
    ADD COLUMN edited BIT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_review_comment_parent FOREIGN KEY (parent_id) REFERENCES review_comment(id);
CREATE INDEX idx_comment_review_parent_id ON review_comment(review_id, parent_id, id);
