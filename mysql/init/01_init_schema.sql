-- ==========================================
--  花卉培育系统 - 幂等数据库初始化脚本
--  兼容已存在数据卷的重复执行场景
-- ==========================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================================
--  1. 花卉品种表
-- ==========================================
CREATE TABLE IF NOT EXISTS flower_variety (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '品种名称',
    alias VARCHAR(100) DEFAULT NULL COMMENT '别名',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类（草本/木本/多肉等）',
    germination_days INT DEFAULT NULL COMMENT '萌芽天数',
    seedling_days INT DEFAULT NULL COMMENT '成苗天数',
    description TEXT COMMENT '品种描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='花卉品种表';

-- ==========================================
--  2. 培育阶段表
-- ==========================================
CREATE TABLE IF NOT EXISTS growth_stage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    stage_code VARCHAR(50) NOT NULL UNIQUE COMMENT '阶段编码',
    stage_name VARCHAR(50) NOT NULL COMMENT '阶段名称',
    stage_order INT NOT NULL COMMENT '阶段顺序',
    description VARCHAR(255) DEFAULT NULL COMMENT '阶段描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_stage_order (stage_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培育阶段表';

-- ==========================================
--  3. 种子信息表
-- ==========================================
CREATE TABLE IF NOT EXISTS seed_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    source_type VARCHAR(20) NOT NULL COMMENT '来源类型（PURCHASE购入/HARVEST采收）',
    acquire_time DATE NOT NULL COMMENT '购入/采收时间',
    storage_location VARCHAR(100) NOT NULL COMMENT '存放位置',
    initial_quantity INT NOT NULL COMMENT '初始数量',
    remaining_quantity INT NOT NULL COMMENT '剩余数量',
    supplier VARCHAR(100) DEFAULT NULL COMMENT '供应商/采收批次',
    shelf_life INT DEFAULT NULL COMMENT '保质期（月）',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_variety_id (variety_id),
    INDEX idx_acquire_time (acquire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种子信息表';

-- ==========================================
--  4. 播种记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS sowing_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    seed_id BIGINT NOT NULL COMMENT '种子ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    sowing_time DATETIME NOT NULL COMMENT '播种时间',
    soil_ratio VARCHAR(200) NOT NULL COMMENT '盆土配比',
    covering_thickness DECIMAL(5,2) DEFAULT NULL COMMENT '覆土厚度（cm）',
    watering_plan VARCHAR(500) NOT NULL COMMENT '初期浇水方案',
    sowing_quantity INT NOT NULL COMMENT '播种数量',
    container_type VARCHAR(50) DEFAULT NULL COMMENT '播种容器类型',
    environment_temp DECIMAL(5,2) DEFAULT NULL COMMENT '环境温度',
    environment_humidity DECIMAL(5,2) DEFAULT NULL COMMENT '环境湿度',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seed_id (seed_id),
    INDEX idx_sowing_time (sowing_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='播种记录表';

-- ==========================================
--  5. 生长阶段跟踪表
-- ==========================================
CREATE TABLE IF NOT EXISTS growth_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sowing_id BIGINT NOT NULL COMMENT '播种记录ID',
    stage_code VARCHAR(50) NOT NULL COMMENT '阶段编码',
    stage_name VARCHAR(50) NOT NULL COMMENT '阶段名称',
    record_time DATETIME NOT NULL COMMENT '记录时间',
    plant_height DECIMAL(8,2) DEFAULT NULL COMMENT '株高（cm）',
    leaf_count INT DEFAULT NULL COMMENT '叶片数',
    root_development VARCHAR(100) DEFAULT NULL COMMENT '根系发育情况',
    health_status VARCHAR(50) DEFAULT NULL COMMENT '健康状态',
    temperature DECIMAL(5,2) DEFAULT NULL COMMENT '温度',
    humidity DECIMAL(5,2) DEFAULT NULL COMMENT '湿度',
    light_hours DECIMAL(4,1) DEFAULT NULL COMMENT '光照时长',
    fertilization VARCHAR(200) DEFAULT NULL COMMENT '施肥情况',
    estimated_survival INT DEFAULT NULL COMMENT '存活估算数量（株）',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sowing_id (sowing_id),
    INDEX idx_stage_code (stage_code),
    INDEX idx_record_time (record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生长阶段跟踪表';

-- ==========================================
--  6. 移栽分盆记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS transplant_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sowing_id BIGINT NOT NULL COMMENT '播种记录ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    transplant_time DATETIME NOT NULL COMMENT '移栽时间',
    pot_specification VARCHAR(100) NOT NULL COMMENT '花盆规格',
    soil_ratio VARCHAR(200) DEFAULT NULL COMMENT '盆土配比',
    transplant_quantity INT NOT NULL COMMENT '移栽数量',
    cumulative_quantity INT DEFAULT NULL COMMENT '累计分盆数量（株）',
    recovery_tips TEXT COMMENT '缓苗养护要点',
    light_requirement VARCHAR(100) DEFAULT NULL COMMENT '光照要求',
    watering_frequency VARCHAR(100) DEFAULT NULL COMMENT '浇水频率',
    fertilization_plan VARCHAR(200) DEFAULT NULL COMMENT '施肥计划',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sowing_id (sowing_id),
    INDEX idx_transplant_time (transplant_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='移栽分盆记录表';

-- ==========================================
--  7. 种子分组表
-- ==========================================
CREATE TABLE IF NOT EXISTS seed_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    group_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分组编码',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类（草本/木本/多肉等）',
    sort_order INT DEFAULT NULL COMMENT '排序顺序',
    description VARCHAR(255) DEFAULT NULL COMMENT '分组描述',
    location VARCHAR(100) DEFAULT NULL COMMENT '存放位置',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sort_order (sort_order),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='种子分组表';

-- ==========================================
--  初始化基础数据（幂等插入）
-- ==========================================

-- 花卉品种（使用 INSERT IGNORE 保证幂等）
INSERT IGNORE INTO flower_variety (id, name, alias, category, germination_days, seedling_days, description) VALUES
(1,  '矮牵牛',       '碧冬茄',     '草本', 7,  45, '茄科碧冬茄属，多年生草本，常作一二年生栽培'),
(2,  '向日葵',       '朝阳花',     '草本', 5,  60, '菊科向日葵属，一年生草本，喜阳光充足'),
(3,  '满天星',       '圆锥石头花', '草本', 10, 70, '石竹科石头花属，多年生草本，花小而繁密'),
(4,  '薄荷',         '银丹草',     '草本', 10, 30, '唇形科薄荷属，多年生草本，具有清凉香气'),
(5,  '多肉-玉露',    '玉露',       '多肉', 15, 90, '阿福花科瓦苇属，多年生多肉植物，喜散射光'),
(6,  '番茄',         '西红柿',     '草本', 6,  50, '茄科茄属，一年生草本，果菜兼用'),
(7,  '三色堇',       '猫儿脸',     '草本', 10, 60, '堇菜科堇菜属，一二年生草本，花形似蝴蝶'),
(8,  '凤仙花',       '指甲花',     '草本', 7,  50, '凤仙花科凤仙花属，一年生草本'),
(9,  '波斯菊',       '秋英',       '草本', 8,  55, '菊科秋英属，一年生或多年生草本'),
(10, '绿萝',         '黄金葛',     '草本', 15, 40, '天南星科麒麟叶属，多年生常绿藤本');

-- 修正自增起始值，避免插入指定 ID 后自增冲突
ALTER TABLE flower_variety AUTO_INCREMENT = 11;

-- 标准培育阶段（使用 INSERT IGNORE + stage_code 唯一索引保证幂等）
INSERT IGNORE INTO growth_stage (id, stage_code, stage_name, stage_order, description) VALUES
(1,  'SEED_STORED',    '种子储存', 0, '种子购入或采收后的储存阶段'),
(2,  'SOWN',           '已播种',   1, '种子已播入土中，等待萌芽'),
(3,  'SPROUTING',      '萌芽期',   2, '种子破土，子叶初现'),
(4,  'COTYLEDON',      '子叶期',   3, '子叶展开，真叶未出'),
(5,  'TRUE_LEAF',      '真叶期',   4, '真叶长出，开始光合作用'),
(6,  'SEEDLING',       '成苗期',   5, '幼苗成型，具备基本形态'),
(7,  'ROOT_DEVELOPED', '根系发育', 6, '根系发达，可进行移栽'),
(8,  'TRANSPLANTED',   '移栽缓苗', 7, '已分盆移栽，适应新环境'),
(9,  'GROWING',        '生长期',   8, '植株旺盛生长阶段'),
(10, 'FLOWERING',      '开花期',   9, '进入开花结实阶段');

ALTER TABLE growth_stage AUTO_INCREMENT = 11;

-- 种子分组（使用 INSERT IGNORE 保证幂等）
INSERT IGNORE INTO seed_group (id, group_name, group_code, category, sort_order, description, location) VALUES
(1, '草本种子区', 'HERB_ZONE', '草本', 1, '存放草本花卉种子', '冷藏柜A区'),
(2, '多肉种子区', 'SUCCULENT_ZONE', '多肉', 2, '存放多肉植物种子', '冷藏柜B区'),
(3, '木本种子区', 'WOODY_ZONE', '木本', 3, '存放木本花卉种子', '常温架C区'),
(4, '待检测区', 'PENDING_ZONE', NULL, 4, '新购入待检测的种子', '检测台D区');

ALTER TABLE seed_group AUTO_INCREMENT = 5;

-- ==========================================
--  8. 幂等 Schema 迁移（兼容已有数据库）
-- ==========================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_not_exists$$

CREATE PROCEDURE add_column_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO column_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF column_exists = 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'),
            '` ', p_column_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 为 growth_tracking 添加 estimated_survival 列
CALL add_column_if_not_exists('growth_tracking', 'estimated_survival', 'INT DEFAULT NULL COMMENT \'存活估算数量（株）\'');

-- 为 transplant_record 添加 cumulative_quantity 列
CALL add_column_if_not_exists('transplant_record', 'cumulative_quantity', 'INT DEFAULT NULL COMMENT \'累计分盆数量（株）\'');

-- 为 seed_info 添加 group_id 列
CALL add_column_if_not_exists('seed_info', 'group_id', 'BIGINT DEFAULT NULL COMMENT \'所属分组ID\'');

-- 为 seed_info 添加 sort_order 列
CALL add_column_if_not_exists('seed_info', 'sort_order', 'INT DEFAULT NULL COMMENT \'组内排序\'');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- ==========================================
--  9. 批次发芽观察表
-- ==========================================
CREATE TABLE IF NOT EXISTS germination_observation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sowing_id BIGINT NOT NULL COMMENT '播种记录ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    observation_date DATE NOT NULL COMMENT '观察日期',
    germinated_count INT NOT NULL DEFAULT 0 COMMENT '发芽数量',
    not_germinated_count INT NOT NULL DEFAULT 0 COMMENT '未发芽数量',
    germination_rate DECIMAL(5,2) DEFAULT NULL COMMENT '发芽率（%）',
    anomaly_note TEXT COMMENT '异常说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sowing_id (sowing_id),
    INDEX idx_observation_date (observation_date),
    UNIQUE INDEX uk_sowing_date (sowing_id, observation_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次发芽观察表';

-- ==========================================
--  10. 家庭育苗计划表
-- ==========================================
CREATE TABLE IF NOT EXISTS seedling_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    seed_id BIGINT NOT NULL COMMENT '种子ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    germination_days INT DEFAULT NULL COMMENT '发芽天数（来自品种）',
    seedling_days INT DEFAULT NULL COMMENT '育苗天数（来自品种）',
    shelf_life INT DEFAULT NULL COMMENT '保质期（月，来自种子）',
    planned_sow_date DATE DEFAULT NULL COMMENT '计划播种日期',
    planned_transplant_date DATE DEFAULT NULL COMMENT '计划移栽日期',
    expire_date DATE DEFAULT NULL COMMENT '种子过期日期',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态（PENDING待执行/SOWING已播种/TRANSPLANTED已移栽/DONE已完成/CANCELLED已取消）',
    actual_sow_date DATE DEFAULT NULL COMMENT '实际播种日期',
    actual_transplant_date DATE DEFAULT NULL COMMENT '实际移栽日期',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_seed_id (seed_id),
    INDEX idx_variety_id (variety_id),
    INDEX idx_status (status),
    INDEX idx_planned_sow_date (planned_sow_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='家庭育苗计划表';

SET FOREIGN_KEY_CHECKS = 1;

-- ==========================================
--  11. 发芽异常处理表
-- ==========================================
CREATE TABLE IF NOT EXISTS germination_anomaly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sowing_id BIGINT NOT NULL COMMENT '播种记录ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    observation_id BIGINT DEFAULT NULL COMMENT '关联的发芽观察记录ID',
    baseline_germination_rate DECIMAL(5,2) NOT NULL COMMENT '品种基准发芽率（%）',
    actual_germination_rate DECIMAL(5,2) NOT NULL COMMENT '实际发芽率（%）',
    rate_diff DECIMAL(5,2) NOT NULL COMMENT '发芽率差值（%）',
    anomaly_level VARCHAR(20) NOT NULL DEFAULT 'MILD' COMMENT '异常等级（MILD轻度/MODERATE中度/SEVERE严重）',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态（PENDING待处理/PROCESSING处理中/RESOLVED已解决/CLOSED已关闭）',
    action_taken TEXT COMMENT '处理措施',
    handler VARCHAR(50) DEFAULT NULL COMMENT '处理人',
    handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
    result_note TEXT COMMENT '处理结果说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sowing_id (sowing_id),
    INDEX idx_status (status),
    INDEX idx_anomaly_level (anomaly_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发芽异常处理表';

-- ==========================================
--  12. 移栽恢复记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS transplant_recovery_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    transplant_id BIGINT NOT NULL COMMENT '移栽记录ID',
    sowing_id BIGINT NOT NULL COMMENT '播种记录ID',
    variety_id BIGINT NOT NULL COMMENT '花卉品种ID',
    variety_name VARCHAR(100) NOT NULL COMMENT '品种名称（冗余）',
    record_date DATE NOT NULL COMMENT '记录日期',
    recovery_days INT NOT NULL COMMENT '移栽后天数',
    leaf_status VARCHAR(50) DEFAULT NULL COMMENT '叶片状态（正常/萎蔫/发黄/脱落等）',
    leaf_count INT DEFAULT NULL COMMENT '叶片数量',
    watering_done TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已浇水',
    watering_amount DECIMAL(6,2) DEFAULT NULL COMMENT '浇水量（ml）',
    health_status VARCHAR(50) DEFAULT NULL COMMENT '整体健康状态',
    temperature DECIMAL(5,2) DEFAULT NULL COMMENT '环境温度（℃）',
    humidity DECIMAL(5,2) DEFAULT NULL COMMENT '环境湿度（%）',
    light_hours DECIMAL(4,1) DEFAULT NULL COMMENT '光照时长（小时）',
    notes TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_transplant_id (transplant_id),
    INDEX idx_sowing_id (sowing_id),
    INDEX idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='移栽恢复记录表';

-- ==========================================
--  Schema 迁移（兼容已有数据库）
-- ==========================================

DELIMITER $$

DROP PROCEDURE IF EXISTS add_column_if_not_exists$$

CREATE PROCEDURE add_column_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition TEXT
)
BEGIN
    DECLARE column_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO column_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name;

    IF column_exists = 0 THEN
        SET @sql = CONCAT(
            'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
            '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'),
            '` ', p_column_definition
        );
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 为 flower_variety 添加基准发芽率字段
CALL add_column_if_not_exists('flower_variety', 'baseline_germination_rate', 'DECIMAL(5,2) DEFAULT 70.00 COMMENT \'基准发芽率（%）\'');

-- 为 sowing_record 添加批次状态字段
CALL add_column_if_not_exists('sowing_record', 'batch_status', 'VARCHAR(20) NOT NULL DEFAULT \'NORMAL\' COMMENT \'批次状态（NORMAL正常/ABNORMAL异常/RESOLVED已恢复）\'');

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
