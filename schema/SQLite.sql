
-- Table structure for table `bandwidths`
DROP TABLE IF EXISTS `bandwidths`;
CREATE TABLE `bandwidths` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `map_id` INT(10) NOT NULL,
    `weights_identification` VARCHAR(255) NOT NULL,
    `kernel_identification` VARCHAR(255) NOT NULL,
    `distance_identification` VARCHAR(255) NOT NULL,
    `estimator_identification` VARCHAR(255) NOT NULL,
    `bandwidth` DECIMAL(65,30) NOT NULL
);
CREATE UNIQUE INDEX `index_on_map_id_identifications` ON `bandwidths` (`map_id`,`weights_identification`,`kernel_identification`,`distance_identification`,`estimator_identification`);


-- Table structure for table `border_coordinates`
DROP TABLE IF EXISTS `border_coordinates`;
CREATE TABLE `border_coordinates` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `border_id` INT(10) NOT NULL,
    `order_index` INT(10) NOT NULL,
    `latitude` DOUBLE NOT NULL,
    `longitude` DOUBLE NOT NULL
);
CREATE UNIQUE INDEX `index_border_coordinates` ON `border_coordinates` (`id` ASC);


-- Table structure for table `borders`
DROP TABLE IF EXISTS `borders`;
CREATE TABLE `borders` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_borders` ON `borders` (`id` ASC);


-- Table structure for table `categories`
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `parent_id` INT(10) NULL,
    `lft` INT(10) NOT NULL,
    `rgt` INT(10) NOT NULL,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_categories` ON `categories` (`id` ASC);
CREATE INDEX `index_on_lft_rgt` ON `categories` (`lft`,`rgt`);
CREATE INDEX `index_on_parent_id`  ON `categories` (`parent_id`);


-- Table structure for table `categories_maps`
DROP TABLE IF EXISTS `categories_maps`;
CREATE TABLE `categories_maps` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `category_id` INT(10) NOT NULL,
    `map_id` INT(10) NOT NULL
);
CREATE UNIQUE INDEX `index_categories_maps` ON `categories_maps` (`id` ASC);


-- Table structure for table `configuration_options`
DROP TABLE IF EXISTS `configuration_options`;
CREATE TABLE `configuration_options` (
	`id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) UNIQUE,
    `value` TEXT NOT NULL
);
CREATE UNIQUE INDEX `index_configuration_options` ON `configuration_options` (`name` ASC);


-- Table structure for table `distances`
DROP TABLE IF EXISTS `distances`;
CREATE TABLE `distances` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `type` VARCHAR(255) NOT NULL,
    `identification` VARCHAR(255) NULL
);
CREATE UNIQUE INDEX `index_distances` ON `distances` (`id` ASC);


-- Table structure for table `groups`
DROP TABLE IF EXISTS `groups`;
CREATE TABLE `groups` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_groups` ON `groups` (`id` ASC);


-- Table structure for table `groups_maps`
DROP TABLE IF EXISTS `groups_maps`;
CREATE TABLE `groups_maps` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `group_id` INT(10) NOT NULL,
    `map_id` INT(10) NOT NULL
);
CREATE UNIQUE INDEX `index_groups_maps` ON `groups_maps` (`id` ASC);
CREATE INDEX `index_on_group_id` ON `groups_maps` (`group_id`);


-- Table structure for table `informants`
DROP TABLE IF EXISTS `informants`;
CREATE TABLE `informants` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `location_id` INT(10) NOT NULL,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_informants` ON `informants` (`id` ASC);
CREATE INDEX `index_on_location_id` ON `informants` (`location_id`);


-- Table structure for table `interview_answers`
DROP TABLE IF EXISTS `interview_answers`;
CREATE TABLE `interview_answers` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `interviewer_id` INT(10) NOT NULL,
    `informant_id` INT(10) NOT NULL,
    `variant_id` INT(10) NOT NULL
);
CREATE UNIQUE INDEX `index_interview_answers` ON `interview_answers` (`id` ASC);
CREATE INDEX `index_on_informant_id` ON `interview_answers` (`informant_id`);
CREATE INDEX `index_on_variant_id` ON `interview_answers` (`variant_id`);


-- Table structure for table `interviewers`
DROP TABLE IF EXISTS `interviewers`;
CREATE TABLE `interviewers` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_interviewers` ON `interviewers` (`id` ASC);


-- Table structure for table `levels`
DROP TABLE IF EXISTS `levels`;
CREATE TABLE `levels` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_levels` ON `levels` (`id` ASC);


-- Table structure for table `location_distances`
DROP TABLE IF EXISTS `location_distances`;
CREATE TABLE `location_distances` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `distance_id` INT(10) NOT NULL,
    `location_id1` INT(10) NOT NULL,
    `location_id2` INT(10) NOT NULL,
    `distance` DOUBLE NOT NULL
);
CREATE UNIQUE INDEX `index_location_distances` ON `location_distances` (`id` ASC);
CREATE INDEX `index_on_distance_id_location_ids` ON `location_distances` (`distance_id`,`location_id1`,`location_id2`);


-- Table structure for table `locations`
DROP TABLE IF EXISTS `locations`;
CREATE TABLE `locations` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `code` VARCHAR(255) NOT NULL,
    `latitude` DOUBLE NOT NULL,
    `longitude` DOUBLE NOT NULL
);
CREATE UNIQUE INDEX `index_locations` ON `locations` (`id` ASC);


-- Table structure for table `maps`
DROP TABLE IF EXISTS `maps`;
CREATE TABLE `maps` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `name` VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX `index_maps` ON `maps` (`id` ASC);


-- Table structure for table `tags`
DROP TABLE IF EXISTS `tags`;
CREATE TABLE `tags` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `parent_type` VARCHAR(255) NOT NULL,
    `parent_id` INT(10) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `value` TEXT NOT NULL
);
CREATE UNIQUE INDEX `index_tags` ON `tags` (`id` ASC);
CREATE INDEX `index_on_parent_type_name_parent_id` ON `tags` (`parent_type`,`name`,`parent_id`);


-- Table structure for table `variants`
DROP TABLE IF EXISTS `variants`;
CREATE TABLE `variants` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `map_id` INT(10) NOT NULL,
    `name` VARCHAR(4095) NOT NULL
);
CREATE UNIQUE INDEX `index_variants` ON `variants` (`id` ASC);
CREATE INDEX `index_on_map_id_name` ON `variants` (`map_id`,`name`);


-- Table structure for table `variants_mappings`
DROP TABLE IF EXISTS `variants_mappings`;
CREATE TABLE `variants_mappings` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT,
    `variant_id` INT(10) NOT NULL,
    `level_id` INT(10) NOT NULL,
    `to_variant_id` INT(10) NULL
);
CREATE UNIQUE INDEX `index_variants_mappings` ON `variants_mappings` (`id` ASC);
CREATE INDEX `index_on_to_variant_id` ON `variants_mappings` (`to_variant_id`);
CREATE INDEX `index_on_variant_id_level_id` ON `variants_mappings` (`variant_id`,`level_id`);

