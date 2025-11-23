package com.salemale.global.external.naver;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NaverShopSearchResponse {

    private int total;
    private int start;
    private int display;
    private List<Item> items;

    @Getter @Setter
    public static class Item {
        private String title;
        private String link;
        private String image;
        private String lprice;
        private String hprice;
        private String mallName;
        private String productId;
        private String brand;
        private String maker;
    }
}
