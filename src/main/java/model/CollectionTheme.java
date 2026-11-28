package model;

import java.util.List;

/** Editorial collections are independent of product categories. */
public final class CollectionTheme {
    private static final List<CollectionTheme> BUILT_IN = List.of(
            new CollectionTheme("autumn-winter", "Thu đông", "Autumn / Winter", "Ấm áp trong từng lớp phối.", "A warmer take on everyday layers.", "fa-leaf"),
            new CollectionTheme("summer", "Summer", "Summer", "Nhẹ nhàng đón những ngày nắng.", "Light pieces for sun-filled days.", "fa-sun"),
            new CollectionTheme("party", "Tiệc", "Party", "Một chút nổi bật cho dịp đặc biệt.", "Make an entrance on special occasions.", "fa-champagne-glasses"),
            new CollectionTheme("beach", "Bãi biển", "Beach", "Mang tinh thần kỳ nghỉ vào tủ đồ.", "Bring a little holiday into your wardrobe.", "fa-umbrella-beach"),
            new CollectionTheme("office", "Công sở", "Office", "Chỉn chu, tự tin mỗi ngày đi làm.", "Considered style for your working week.", "fa-briefcase"));

    private final String slug;
    private final String nameVi;
    private final String nameEn;
    private final String descriptionVi;
    private final String descriptionEn;
    private final String icon;
    private final Long customId;

    private CollectionTheme(String slug, String nameVi, String nameEn, String descriptionVi, String descriptionEn, String icon) {
        this(slug, nameVi, nameEn, descriptionVi, descriptionEn, icon, null);
    }

    private CollectionTheme(String slug, String nameVi, String nameEn, String descriptionVi, String descriptionEn,
            String icon, Long customId) {
        this.slug = slug;
        this.nameVi = nameVi;
        this.nameEn = nameEn;
        this.descriptionVi = descriptionVi;
        this.descriptionEn = descriptionEn;
        this.icon = icon;
        this.customId = customId;
    }

    public String getSlug() { return slug; }
    public String getNameVi() { return nameVi; }
    public String getNameEn() { return nameEn; }
    public String getDescriptionVi() { return descriptionVi; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getIcon() { return icon; }
    public Long getCustomId() { return customId; }
    public static List<CollectionTheme> builtIn() { return BUILT_IN; }

    public static CollectionTheme custom(long id, String title) {
        return new CollectionTheme("custom-" + id, title, title, "", "", "fa-tags", id);
    }

    public CollectionTheme withDescription(String description) {
        return new CollectionTheme(slug, nameVi, nameEn, description, description, icon, customId);
    }

    public static CollectionTheme findBySlug(List<CollectionTheme> themes, String slug) {
        return themes.stream().filter(theme -> theme.slug.equalsIgnoreCase(slug)).findFirst().orElse(null);
    }
}
