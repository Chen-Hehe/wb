-- 为微博表添加 AI 审核字段
-- 执行时间：2026-04-10

-- 添加审核状态字段：0-待审核 1-通过 2-不通过
ALTER TABLE weibos ADD COLUMN wb_pass TINYINT DEFAULT 0 COMMENT '审核状态：0-待审核 1-通过 2-不通过';

-- 添加审核备注字段
ALTER TABLE weibos ADD COLUMN wb_remark VARCHAR(500) DEFAULT NULL COMMENT '审核备注/不通过原因';

-- 说明：
-- 1. 现有数据默认 wb_pass=0（待审核），需要手动更新或重新发布
-- 2. 新发布的微博会自动进行 AI 审核并设置 wb_pass 和 wb_remark
-- 3. 查询时只显示 wb_pass=1（审核通过）的数据
