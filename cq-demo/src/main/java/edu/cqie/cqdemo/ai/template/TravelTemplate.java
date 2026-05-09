package edu.cqie.cqdemo.ai.template;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TravelTemplate {

    BUDGET("穷游攻略", "budget",
            "请生成一份经济实惠的重庆穷游攻略，要求如下：\n" +
            "- 优先推荐免费景点和低价美食\n" +
            "- 提供经济型住宿推荐（青旅、经济酒店）\n" +
            "- 推荐公共交通出行方式\n" +
            "- 标注每个景点的门票价格\n" +
            "- 提供省钱小贴士\n" +
            "- 总预算控制在较低水平"),

    LUXURY("豪华攻略", "luxury",
            "请生成一份高端奢华的重庆旅游攻略，要求如下：\n" +
            "- 推荐高端酒店和特色民宿\n" +
            "- 推荐米其林餐厅和高端美食\n" +
            "- 提供私人定制行程建议\n" +
            "- 推荐高端SPA和休闲体验\n" +
            "- 提供VIP通道和专属服务信息\n" +
            "- 注重品质和舒适度"),

    FAMILY("亲子攻略", "family",
            "请生成一份适合亲子游的重庆旅游攻略，要求如下：\n" +
            "- 推荐适合儿童的景点和游乐设施\n" +
            "- 提供亲子互动体验项目\n" +
            "- 推荐家庭友好型餐厅\n" +
            "- 考虑儿童的体力和兴趣\n" +
            "- 提供安全提示和注意事项\n" +
            "- 安排适当的休息时间"),

    COUPLE("情侣攻略", "couple",
            "请生成一份浪漫的重庆情侣旅游攻略，要求如下：\n" +
            "- 推荐浪漫景点和拍照打卡地\n" +
            "- 提供情侣约会餐厅推荐\n" +
            "- 安排浪漫的夜景行程\n" +
            "- 推荐特色体验（如江边漫步、夜景观赏）\n" +
            "- 提供纪念日或求婚建议\n" +
            "- 注重氛围和体验感"),

    CULTURAL("文化深度游", "cultural",
            "请生成一份重庆文化深度游攻略，要求如下：\n" +
            "- 推荐历史文化景点和博物馆\n" +
            "- 介绍重庆的历史背景和文化特色\n" +
            "- 推荐老街区和传统建筑\n" +
            "- 提供非遗体验和民俗活动\n" +
            "- 推荐本地文化讲座或导览\n" +
            "- 注重文化内涵和教育意义"),

    FOOD("美食探店攻略", "food",
            "请生成一份重庆美食探店攻略，要求如下：\n" +
            "- 推荐地道的重庆火锅和小面\n" +
            "- 介绍各类特色小吃和夜市\n" +
            "- 提供网红餐厅和老字号推荐\n" +
            "- 标注人均消费和推荐菜品\n" +
            "- 提供美食地图和路线规划\n" +
            "- 注重口味多样性和体验感"),

    PHOTO("摄影打卡攻略", "photo",
            "请生成一份重庆摄影打卡攻略，要求如下：\n" +
            "- 推荐最佳摄影地点和机位\n" +
            "- 提供最佳拍摄时间（光线、人流）\n" +
            "- 推荐特色建筑和街景\n" +
            "- 提供摄影技巧和构图建议\n" +
            "- 安排日出日落拍摄行程\n" +
            "- 注重视觉效果和创意"),

    CUSTOM("自定义攻略", "custom", "");

    private final String name;
    private final String code;
    private final String promptTemplate;

    public static TravelTemplate fromCode(String code) {
        for (TravelTemplate template : values()) {
            if (template.code.equals(code)) {
                return template;
            }
        }
        return CUSTOM;
    }

    public static TravelTemplate fromName(String name) {
        for (TravelTemplate template : values()) {
            if (template.name.equals(name)) {
                return template;
            }
        }
        return CUSTOM;
    }
}
