package com.salemale.domain.search.entity;

import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "search")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Search extends BaseEntity {

    @Id
    @Column(name = "keyword", nullable = false, length = 30)
    private String keyword;

    public Search(String keyword) {
        this.keyword = keyword;
    }

    public static Search of(String keyword) {
        return new Search(keyword);
    }

    // Getter
    public String getKeyword() {
        return keyword;
    }

    // Setter
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
