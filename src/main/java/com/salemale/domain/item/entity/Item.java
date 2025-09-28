package com.salemale.domain.item.entity;

import com.salemale.domain.region.entity.Region;
import com.salemale.domain.user.entity.User;
import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "title", nullable = false, length = 30)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "start_price", nullable = false)
    private Integer startPrice;

    @Column(name = "time", nullable = false)
    private LocalDateTime time;

    @Column(name = "total_time", nullable = false)
    private LocalDateTime totalTime;

    @Column(name = "photo_url", nullable = false, length = 100)
    private String photoUrl;

    public Item() {
    }

    public Item(User seller, Region region, String name, String title, String description,
                Category category, Integer price, Integer startPrice, LocalDateTime time,
                LocalDateTime totalTime, String photoUrl) {
        this.seller = seller;
        this.region = region;
        this.name = name;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.startPrice = startPrice;
        this.time = time;
        this.totalTime = totalTime;
        this.photoUrl = photoUrl;
    }

    public static Item of(User seller, Region region, String name, String title, String description,
                          Category category, Integer price, Integer startPrice, LocalDateTime time,
                          LocalDateTime totalTime, String photoUrl) {
        return new Item(seller, region, name, title, description, category, price, startPrice, time, totalTime, photoUrl);
    }

    public void updateItem(String name, String title, String description, Category category,
                           Integer price, String photoUrl) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.photoUrl = photoUrl;
    }

    public void updatePrice(Integer price) {
        this.price = price;
    }

    public void updateTime(LocalDateTime time) {
        this.time = time;
    }

    // Getter
    public Long getItemId() {
        return itemId;
    }

    public User getSeller() {
        return seller;
    }

    public Region getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getStartPrice() {
        return startPrice;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public LocalDateTime getTotalTime() {
        return totalTime;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    // Setter
    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public void setStartPrice(Integer startPrice) {
        this.startPrice = startPrice;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public void setTotalTime(LocalDateTime totalTime) {
        this.totalTime = totalTime;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public enum Category {
        ELECTRONICS, CLOTHING, BOOKS, HOME, SPORTS, BEAUTY, 
        AUTOMOTIVE, TOYS, FOOD, HEALTH, PET, GARDEN, 
        MUSIC, ART, COLLECTIBLES, ANTIQUES, JEWELRY, OTHER
    }
}
