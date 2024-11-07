-- 插入课程分类数据
INSERT INTO course_category (id, name, parent_id, level, create_time, update_time) VALUES
(1, '后端开发', 0, 1, NOW(), NOW()),
(2, 'Java开发', 1, 2, NOW(), NOW()),
(3, '前端开发', 0, 1, NOW(), NOW()),
(4, 'Vue开发', 3, 2, NOW(), NOW());

-- 插入课程基本信息
INSERT INTO course_base (name, brief, mt, st, charge, status, valid, create_time, update_time) VALUES
('Java基础课程', 'Java语言基础入门', 1, 2, '201001', '202001', true, NOW(), NOW()),
('Vue3实战课程', 'Vue3框架实战', 3, 4, '201002', '202001', true, NOW(), NOW());

-- 插入课程营销信息
INSERT INTO course_market (id, charge, price, valid, create_time, update_time) VALUES
(1, '201001', 0.00, true, NOW(), NOW()),
(2, '201002', 99.00, true, NOW(), NOW()); 