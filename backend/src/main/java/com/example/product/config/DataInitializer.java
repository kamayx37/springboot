package com.example.product.config;

import com.example.product.entity.*;
import com.example.product.repository.*;
import com.example.product.search.ProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 演示数据初始化：权限树、角色、测试账号（BCrypt 加密）、商品分类与商品数据。
 * <p>幂等：数据库已有数据时不会重复写入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final SysPermissionRepository permissionRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserRepository userRepository;
    private final BizCategoryRepository categoryRepository;
    private final BizProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductIndexService productIndexService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() == 0) {
            initPermissionsAndAccounts();
        } else {
            log.info("检测到已有权限数据，跳过 RBAC 初始化");
        }
        if (categoryRepository.count() == 0) {
            initCategoriesAndProducts();
        } else {
            log.info("检测到已有分类数据，跳过业务数据初始化");
        }
        // 尝试同步一次 Elasticsearch 索引（ES 未就绪时忽略，可在界面手动重建）
        try {
            if (productRepository.count() > 0) {
                productIndexService.rebuildAll();
            }
        } catch (Exception e) {
            log.warn("启动时自动重建 ES 索引失败（可在界面手动提交重建任务）: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ RBAC

    private void initPermissionsAndAccounts() {
        log.info("开始初始化权限、角色与测试账号...");

        // 系统管理
        SysPermission sys = permission("sys", "系统管理", "MENU", 0L, "/system", "Setting", 1);
        SysPermission userMenu = permission("sys:user", "用户管理", "MENU", sys.getId(), "/system/users", "User", 1);
        permission("user:view", "查看用户", "API", userMenu.getId(), null, null, 1);
        permission("user:create", "新增用户", "BUTTON", userMenu.getId(), null, null, 2);
        permission("user:update", "修改用户", "BUTTON", userMenu.getId(), null, null, 3);
        permission("user:delete", "删除用户", "BUTTON", userMenu.getId(), null, null, 4);
        permission("user:assign", "分配角色", "BUTTON", userMenu.getId(), null, null, 5);

        SysPermission roleMenu = permission("sys:role", "角色管理", "MENU", sys.getId(), "/system/roles", "Avatar", 2);
        permission("role:view", "查看角色", "API", roleMenu.getId(), null, null, 1);
        permission("role:create", "新增角色", "BUTTON", roleMenu.getId(), null, null, 2);
        permission("role:update", "修改角色", "BUTTON", roleMenu.getId(), null, null, 3);
        permission("role:delete", "删除角色", "BUTTON", roleMenu.getId(), null, null, 4);
        permission("role:assign", "分配权限", "BUTTON", roleMenu.getId(), null, null, 5);

        SysPermission permMenu = permission("sys:permission", "权限管理", "MENU", sys.getId(), "/system/permissions", "Key", 3);
        permission("permission:view", "查看权限", "API", permMenu.getId(), null, null, 1);
        permission("permission:create", "新增权限", "BUTTON", permMenu.getId(), null, null, 2);
        permission("permission:update", "修改权限", "BUTTON", permMenu.getId(), null, null, 3);
        permission("permission:delete", "删除权限", "BUTTON", permMenu.getId(), null, null, 4);

        SysPermission logMenu = permission("sys:log", "操作日志", "MENU", sys.getId(), "/system/logs", "Document", 4);
        permission("log:view", "查看日志", "API", logMenu.getId(), null, null, 1);
        permission("log:clear", "清空日志", "BUTTON", logMenu.getId(), null, null, 2);

        // 商品中心
        SysPermission goods = permission("goods", "商品中心", "MENU", 0L, "/goods", "Goods", 2);
        SysPermission productMenu = permission("goods:product", "商品管理", "MENU", goods.getId(), "/goods/products", "ShoppingCart", 1);
        permission("product:view", "查看商品", "API", productMenu.getId(), null, null, 1);
        permission("product:create", "新增商品", "BUTTON", productMenu.getId(), null, null, 2);
        permission("product:update", "修改商品", "BUTTON", productMenu.getId(), null, null, 3);
        permission("product:delete", "删除商品", "BUTTON", productMenu.getId(), null, null, 4);

        SysPermission categoryMenu = permission("goods:category", "分类管理", "MENU", goods.getId(), "/goods/categories", "Menu", 2);
        permission("category:view", "查看分类", "API", categoryMenu.getId(), null, null, 1);
        permission("category:create", "新增分类", "BUTTON", categoryMenu.getId(), null, null, 2);
        permission("category:update", "修改分类", "BUTTON", categoryMenu.getId(), null, null, 3);
        permission("category:delete", "删除分类", "BUTTON", categoryMenu.getId(), null, null, 4);

        // 全文检索
        SysPermission searchMenu = permission("search", "全文检索", "MENU", 0L, "/search", "Search", 3);
        permission("search:view", "执行检索", "API", searchMenu.getId(), null, null, 1);
        permission("search:rebuild", "重建索引", "BUTTON", searchMenu.getId(), null, null, 2);

        List<SysPermission> all = permissionRepository.findAll();

        // ---- 角色 ----
        SysRole adminRole = role("ADMIN", "超级管理员", "拥有系统全部权限", 1, all);
        List<SysPermission> productPerms = all.stream()
                .filter(p -> p.getCode().startsWith("product:") || p.getCode().startsWith("category:")
                        || p.getCode().startsWith("search:") || p.getCode().startsWith("goods:"))
                .toList();
        role("PRODUCT_MANAGER", "商品管理员", "负责商品与分类维护，可执行检索与索引重建", 2, productPerms);
        List<SysPermission> viewPerms = all.stream()
                .filter(p -> p.getCode().endsWith(":view") || "search".equals(p.getCode())
                        || "goods".equals(p.getCode()) || "sys".equals(p.getCode()))
                .toList();
        role("VIEWER", "只读访客", "仅可查看数据，不能进行任何写操作", 3, viewPerms);

        // ---- 测试账号（密码全部 BCrypt 加密存储）----
        user("admin", "admin123", "系统管理员", "admin@example.com", "13800000001", adminRole);
        SysRole productRole = roleRepository.findByCode("PRODUCT_MANAGER").orElseThrow();
        user("product", "product123", "商品运营", "product@example.com", "13800000002", productRole);
        SysRole viewerRole = roleRepository.findByCode("VIEWER").orElseThrow();
        user("viewer", "viewer123", "只读访客", "viewer@example.com", "13800000003", viewerRole);

        log.info("RBAC 初始化完成：权限 {} 条，角色 {} 个，用户 {} 个",
                permissionRepository.count(), roleRepository.count(), userRepository.count());
    }

    private SysPermission permission(String code, String name, String type, Long parentId,
                                     String path, String icon, int sort) {
        SysPermission entity = new SysPermission();
        entity.setCode(code);
        entity.setName(name);
        entity.setType(type);
        entity.setParentId(parentId == null ? 0L : parentId);
        entity.setPath(path);
        entity.setIcon(icon);
        entity.setSortOrder(sort);
        return permissionRepository.save(entity);
    }

    private SysRole role(String code, String name, String description, int sort, List<SysPermission> permissions) {
        SysRole entity = new SysRole();
        entity.setCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setStatus(1);
        entity.setSortOrder(sort);
        entity.setPermissions(new HashSet<>(permissions));
        return roleRepository.save(entity);
    }

    private void user(String username, String rawPassword, String nickname, String email,
                      String phone, SysRole role) {
        SysUser entity = new SysUser();
        entity.setUsername(username);
        // 关键：密码使用 BCrypt 加密后入库，数据库里不存在明文
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setNickname(nickname);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setStatus(1);
        entity.setRemark("演示账号");
        entity.setRoles(new HashSet<>(Set.of(role)));
        userRepository.save(entity);
    }


    private void initCategoriesAndProducts() {
        log.info("开始初始化分类与商品数据...");
        Map<String, Long> catIds = new LinkedHashMap<>();

        // 一级分类
        Long electronics = category("电子产品", "electronics", 0L, 1, "手机、电脑、平板等消费电子");
        Long appliance = category("家用电器", "appliance", 0L, 2, "冰箱、洗衣机、厨卫小家电");
        Long clothing = category("服饰鞋包", "clothing", 0L, 3, "男装、女装、鞋靴箱包");
        Long food = category("食品饮料", "food", 0L, 4, "休闲零食与各类饮品");

        // 二级分类
        catIds.put("phone", category("手机通讯", "phone", electronics, 1, "智能手机与通讯配件"));
        catIds.put("laptop", category("电脑办公", "laptop", electronics, 2, "笔记本、台式机与办公外设"));
        catIds.put("tablet", category("平板电脑", "tablet", electronics, 3, "平板与影音设备"));
        catIds.put("fridge", category("冰箱冷柜", "fridge", appliance, 1, "家用冰箱与冷柜"));
        catIds.put("washer", category("洗衣机", "washer", appliance, 2, "滚筒/波轮洗衣机"));
        catIds.put("kitchen", category("厨房小电", "kitchen", appliance, 3, "电饭煲、破壁机等厨房电器"));
        catIds.put("menswear", category("男装男鞋", "menswear", clothing, 1, "男士服装与运动鞋"));
        catIds.put("womenswear", category("女装女鞋", "womenswear", clothing, 2, "女士服装与鞋包"));
        catIds.put("snack", category("休闲零食", "snack", food, 1, "坚果、饼干、肉脯等零食"));
        catIds.put("drink", category("饮料冲调", "drink", food, 2, "饮用水、汽水、乳制品"));

        List<BizProduct> products = new ArrayList<>();
        add(products, catIds, "华为 Mate 60 Pro 智能手机", "P001", "phone", "华为", "6999.00", 120, 1, 3200,
                "搭载新一代麒麟芯片的旗舰智能手机，支持卫星通话，超感知徕卡影像系统，昆仑玻璃抗摔屏幕，超长续航与超级快充。");
        add(products, catIds, "小米 14 Ultra 影像旗舰手机", "P002", "phone", "小米", "6499.00", 88, 1, 2100,
                "徕卡光学四摄，可变光圈主摄，2K 全等深微曲屏，骁龙旗舰平台，支持双向卫星通信与 90W 快充。");
        add(products, catIds, "Apple iPhone 15 Pro Max", "P003", "phone", "Apple", "9999.00", 56, 1, 4500,
                "钛金属机身，A17 Pro 芯片，专业级摄像系统，支持 USB-C 与操作按钮，超瓷晶面板。");
        add(products, catIds, "OPPO Find X7 Ultra 拍照手机", "P004", "phone", "OPPO", "5999.00", 70, 1, 1300,
                "双潜望长焦影像系统，哈苏影像调校，2K 曲面屏，百瓦闪充，旗舰性能。");
        add(products, catIds, "vivo X100 Pro 蔡司影像手机", "P005", "phone", "vivo", "5499.00", 65, 1, 1100,
                "蔡司大底主摄，天玑旗舰芯片，蓝海电池长续航，超清夜景拍摄能力出色。");
        add(products, catIds, "荣耀 Magic6 Pro 智能手机", "P006", "phone", "荣耀", "4399.00", 95, 1, 980,
                "鹰眼相机系统，青海湖电池，护眼曲面屏，支持卫星通信与 AI 智慧服务。");
        add(products, catIds, "三星 Galaxy S24 Ultra", "P007", "phone", "三星", "8999.00", 40, 1, 760,
                "内置 S Pen 手写笔，2 亿像素影像，钛金属边框，Galaxy AI 智能助手。");
        add(products, catIds, "一加 12 性能旗舰手机", "P008", "phone", "一加", "4299.00", 110, 1, 890,
                "2K 东方屏，第三代骁龙 8 平台，哈苏影像，百瓦超级闪充与长寿电池。");
        add(products, catIds, "华为 FreeBuds Pro 3 降噪耳机", "P051", "phone", "华为", "1499.00", 200, 1, 2600,
                "星闪连接技术，双单元双频段降噪，高清空间音频，佩戴舒适长续航。");
        add(products, catIds, "小米智能手环 8", "P052", "phone", "小米", "249.00", 500, 1, 5200,
                "大屏健康监测手环，支持心率血氧睡眠监测，多种运动模式，超长续航。");
        add(products, catIds, "Apple AirPods Pro 2 主动降噪", "P053", "phone", "Apple", "1899.00", 150, 1, 3100,
                "主动降噪与自适应通透模式，个性化空间音频，MagSafe 充电盒。");
        add(products, catIds, "索尼 WH-1000XM5 头戴降噪耳机", "P054", "phone", "索尼", "2899.00", 60, 1, 720,
                "业界领先的降噪效果，轻盈舒适佩戴，支持 LDAC 高解析音质与多点连接。");

        add(products, catIds, "联想 ThinkPad X1 Carbon 商务本", "P009", "laptop", "联想", "12999.00", 30, 1, 420,
                "碳纤维轻薄机身，14 英寸高色域屏幕，经典小红点键盘，商务办公稳定可靠。");
        add(products, catIds, "华为 MateBook X Pro 轻薄本", "P010", "laptop", "华为", "9999.00", 45, 1, 610,
                "3.1K 触控全面屏，超轻金属机身，多屏协同，长续航办公体验。");
        add(products, catIds, "Apple MacBook Pro 14 英寸", "P011", "laptop", "Apple", "16999.00", 25, 1, 530,
                "M3 Pro 芯片，Liquid 视网膜 XDR 显示屏，超长续航，专业创作利器。");
        add(products, catIds, "戴尔 XPS 13 轻薄笔记本", "P012", "laptop", "戴尔", "10999.00", 35, 1, 290,
                "InfinityEdge 微边框屏幕，铝合金机身，键盘手感出色，便携商务首选。");
        add(products, catIds, "华硕 ROG 玩家国度游戏本", "P013", "laptop", "华硕", "13999.00", 20, 1, 380,
                "高刷新率电竞屏，独立显卡强劲性能，冰川散热架构，RGB 背光键盘。");
        add(products, catIds, "小米 Redmi Book Pro 15", "P014", "laptop", "小米", "4999.00", 80, 1, 950,
                "3.2K 高分辨率屏幕，标压处理器，全金属机身，高性价比轻薄本。");
        add(products, catIds, "惠普星 14 青春版笔记本", "P015", "laptop", "惠普", "4599.00", 90, 1, 840,
                "14 英寸微边框屏幕，轻薄便携，长续航，适合学生与日常办公。");
        add(products, catIds, "罗技 MX Master 3S 无线鼠标", "P055", "laptop", "罗技", "699.00", 300, 1, 2800,
                "静音按键，8000DPI 高精度传感器，多设备切换，人体工学设计。");
        add(products, catIds, "联想拯救者 27 英寸电竞显示器", "P056", "laptop", "联想", "1699.00", 75, 1, 640,
                "2K 分辨率 165Hz 高刷新率，IPS 面板，低蓝光护眼，支持升降旋转。");

        add(products, catIds, "Apple iPad Pro 12.9 英寸", "P016", "tablet", "Apple", "8999.00", 40, 1, 880,
                "M2 芯片，mini-LED  Liquid 视网膜 XDR 屏，支持妙控键盘与二代手写笔。");
        add(products, catIds, "华为 MatePad Pro 13.2 平板", "P017", "tablet", "华为", "4999.00", 65, 1, 720,
                "柔性 OLED 大屏，超轻薄机身，多设备协同，生产力与娱乐兼顾。");
        add(products, catIds, "小米平板 6 Pro", "P018", "tablet", "小米", "2799.00", 120, 1, 1500,
                "144Hz 高刷屏，骁龙旗舰平台，四扬声器，轻薄金属一体化机身。");
        add(products, catIds, "三星 Galaxy Tab S9 平板", "P019", "tablet", "三星", "6499.00", 35, 1, 410,
                "动态 AMOLED 2X 屏幕，附赠 S Pen，IP68 防水，DeX 桌面模式。");

        add(products, catIds, "海尔 501 升对开门冰箱", "P020", "fridge", "海尔", "4299.00", 28, 1, 350,
                "风冷无霜，变频压缩机节能静音，干湿分储保鲜，大容量对开门设计。");
        add(products, catIds, "松下 纳米水离子多门冰箱", "P025", "fridge", "松下", "5999.00", 18, 1, 220,
                "纳米水离子保鲜技术，多门分区储存，顶置压缩机释放空间，静音节能。");
        add(products, catIds, "美的 1.5 匹变频空调", "P021", "appliance", "美的", "3299.00", 60, 1, 1200,
                "新一级能效变频，快速冷暖，自清洁功能，静音运行，智能控制。");
        add(products, catIds, "格力云锦 1.5 匹空调", "P022", "appliance", "格力", "3599.00", 55, 1, 1050,
                "新一级能效，双级增焓压缩机，56℃ 高温自清洁，舒适节能。");
        add(products, catIds, "西门子 10 公斤滚筒洗衣机", "P023", "washer", "西门子", "3899.00", 32, 1, 460,
                "变频电机静音耐用，多种洗涤程序，高温筒清洁，智能感知衣物重量。");
        add(products, catIds, "小天鹅 洗烘一体机 10 公斤", "P024", "washer", "小天鹅", "2999.00", 42, 1, 690,
                "洗烘一体省空间，除螨除菌，变频静音，智能投放洗涤剂。");
        add(products, catIds, "海信 75 英寸 4K 激光电视", "P026", "appliance", "海信", "8999.00", 15, 1, 180,
                "超短焦激光投影，护眼大屏，内置音响系统，客厅影院级体验。");
        add(products, catIds, "TCL 量子点 4K 智能电视", "P027", "appliance", "TCL", "4599.00", 38, 1, 520,
                "量子点广色域，高刷新率，远场语音控制，海量影视资源。");
        add(products, catIds, "九阳破壁料理机", "P028", "kitchen", "九阳", "899.00", 150, 1, 1900,
                "高速破壁细腻免滤，多功能料理，静音降噪，一键清洗。");
        add(products, catIds, "苏泊尔球釜电饭煲", "P029", "kitchen", "苏泊尔", "499.00", 220, 1, 2600,
                "球釜内胆受热均匀，多种米饭口感，24 小时预约，易清洁内盖。");
        add(products, catIds, "戴森 V12 无线手持吸尘器", "P030", "appliance", "戴森", "3999.00", 45, 1, 780,
                "激光探测微尘，强劲吸力，整机过滤，多款吸头应对全屋清洁。");
        add(products, catIds, "美的电热水壶 1.7L", "P057", "kitchen", "美的", "129.00", 400, 1, 3800,
                "304 不锈钢内胆，双层防烫，快速沸腾，自动断电保护。");
        add(products, catIds, "格兰仕变频微波炉", "P058", "kitchen", "格兰仕", "499.00", 130, 1, 1400,
                "变频加热均匀，多档火力，解冻烧烤一体，智能菜单一键操作。");
        add(products, catIds, "苏泊尔不粘炒锅 32cm", "P059", "kitchen", "苏泊尔", "299.00", 260, 1, 2100,
                "不粘涂层少油烟，复合锅底导热快，木柄防烫，适用于多种炉具。");

        add(products, catIds, "优衣库男士轻型羽绒服", "P031", "menswear", "优衣库", "599.00", 180, 1, 2400,
                "轻便保暖羽绒外套，可收纳便携，防泼水面料，简约百搭版型。");
        add(products, catIds, "海澜之家商务修身西装", "P032", "menswear", "海澜之家", "1299.00", 90, 1, 860,
                "修身剪裁西装外套，抗皱免烫面料，适合商务通勤与正式场合。");
        add(products, catIds, "耐克空军一号运动鞋", "P033", "menswear", "耐克", "799.00", 210, 1, 3200,
                "经典空军一号板鞋，真皮拼接鞋面，缓震鞋底，百搭潮流单品。");
        add(products, catIds, "阿迪达斯运动连帽卫衣", "P034", "menswear", "阿迪达斯", "459.00", 240, 1, 1800,
                "柔软亲肤棉质面料，经典三条纹设计，宽松版型运动休闲。");
        add(products, catIds, "李宁专业跑步鞋", "P039", "menswear", "李宁", "499.00", 190, 1, 2200,
                "轻质回弹中底，透气网面鞋身，稳定支撑，适合日常与长 distance 跑步。");
        add(products, catIds, "安踏实战篮球鞋", "P040", "menswear", "安踏", "459.00", 175, 1, 1650,
                "高帮包裹脚踝，耐磨橡胶大底，缓震回弹，实战性能出色。");
        add(products, catIds, "太平鸟女士长款风衣", "P035", "womenswear", "太平鸟", "899.00", 120, 1, 980,
                "垂坠感面料，收腰显瘦版型，经典翻领设计，春秋季百搭外套。");
        add(products, catIds, "优衣库女士柔软针织衫", "P036", "womenswear", "优衣库", "299.00", 300, 1, 2900,
                "柔软亲肤针织面料，圆领基础款，多色可选，秋冬内搭外穿皆宜。");
        add(products, catIds, "江南布衣设计感连衣裙", "P037", "womenswear", "江南布衣", "1099.00", 70, 1, 540,
                "原创设计印花，舒适棉麻面料，宽松显瘦版型，文艺气质长裙。");
        add(products, catIds, "百丽女士细跟高跟鞋", "P038", "womenswear", "百丽", "699.00", 140, 1, 1120,
                "优雅细跟设计，柔软羊皮鞋面，防滑耐磨大底，通勤宴会皆适合。");
        add(products, catIds, "富安娜全棉四件套床品", "P060", "clothing", "富安娜", "899.00", 110, 1, 1300,
                "100% 全棉面料，亲肤透气，活性印染不褪色，含被套床单枕套。");

        add(products, catIds, "三只松鼠每日坚果礼盒", "P041", "snack", "三只松鼠", "139.00", 480, 1, 5600,
                "多种坚果与果干科学配比，独立小包装锁鲜，营养早餐与办公零食。");
        add(products, catIds, "良品铺子蜜汁猪肉脯", "P042", "snack", "良品铺子", "69.00", 620, 1, 4300,
                "精选后腿肉慢烤，蜜汁风味，肉香浓郁有嚼劲，追剧解馋零食。");
        add(products, catIds, "百草味芒果干", "P043", "snack", "百草味", "49.00", 700, 1, 3600,
                "精选东南亚芒果，低温烘干保留果香，酸甜适口，无添加色素。");
        add(products, catIds, "乐事薯片混合大礼包", "P044", "snack", "乐事", "59.00", 800, 1, 4800,
                "多种口味组合装，薄脆香酥，聚会分享与居家囤货必备。");
        add(products, catIds, "奥利奥夹心饼干家庭装", "P045", "snack", "奥利奥", "29.00", 900, 1, 6100,
                "经典可可饼干夹心，扭一扭泡一泡，家庭分享装更划算。");
        add(products, catIds, "农夫山泉天然矿泉水整箱", "P046", "drink", "农夫山泉", "55.00", 1000, 1, 8200,
                "天然水源地灌装，弱碱性水质，整箱装适合家庭与办公饮用。");
        add(products, catIds, "可口可乐经典汽水整箱", "P047", "drink", "可口可乐", "45.00", 950, 1, 7600,
                "经典口味碳酸饮料，冰镇饮用更畅爽，聚会囤货常备。");
        add(products, catIds, "元气森林无糖气泡水", "P048", "drink", "元气森林", "79.00", 600, 1, 4100,
                "0 糖 0 脂 0 卡，多种果味选择，清爽气泡口感，健康饮品。");
        add(products, catIds, "星巴克星冰乐即饮咖啡", "P049", "drink", "星巴克", "89.00", 350, 1, 2300,
                "即饮瓶装咖啡饮料，浓郁奶香与咖啡风味，随时随地享用。");
        add(products, catIds, "蒙牛特仑苏纯牛奶整箱", "P050", "drink", "蒙牛", "75.00", 700, 1, 5400,
                "高蛋白纯牛奶，每 100ml 含 3.6g 优质乳蛋白，全家营养早餐。");

        productRepository.saveAll(products);
        log.info("已初始化分类 {} 个、商品 {} 条", categoryRepository.count(), productRepository.count());
    }

    private Long category(String name, String code, Long parentId, int sort, String description) {
        BizCategory entity = new BizCategory();
        entity.setName(name);
        entity.setCode(code);
        entity.setParentId(parentId == null ? 0L : parentId);
        entity.setSortOrder(sort);
        entity.setStatus(1);
        entity.setDescription(description);
        return categoryRepository.save(entity).getId();
    }

    private void add(List<BizProduct> list, Map<String, Long> catIds, String name, String code,
                     String categoryCode, String brand, String price, int stock, int status,
                     int sales, String description) {
        BizProduct p = new BizProduct();
        p.setName(name);
        p.setCode(code);
        p.setCategoryId(catIds.get(categoryCode));
        p.setBrand(brand);
        p.setPrice(new BigDecimal(price));
        p.setStock(stock);
        p.setUnit("件");
        p.setStatus(status);
        p.setSales(sales);
        p.setDescription(description);
        list.add(p);
    }
}
