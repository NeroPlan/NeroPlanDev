-- 계획 하나에 상세 하나 (docs/IMPROVEMENTS.md 1번)
-- 비동기 재배열과 분석하기가 겹쳐 생긴 중복 행을 정리한 뒤 unique 제약 추가

-- 계획별로 가장 최근 행만 남기고 삭제
DELETE FROM plan_detail d
USING plan_detail newer
WHERE d.plan_id = newer.plan_id
  AND d.plan_detail_id < newer.plan_detail_id;

-- ddl-auto: update가 이미 unique 제약을 추가한 DB도 있으므로, plan_id 하나짜리 unique 인덱스가 없을 때만 추가
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
        WHERE i.indrelid = 'plan_detail'::regclass
          AND i.indisunique
          AND i.indnatts = 1
          AND a.attname = 'plan_id'
    ) THEN
        ALTER TABLE plan_detail ADD CONSTRAINT uk_plan_detail_plan_id UNIQUE (plan_id);
    END IF;
END $$;
