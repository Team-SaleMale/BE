package com.salemale.domain.region.entity;

import com.salemale.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_name", nullable = false, length = 50)
    private String regionName;

    @Column(name = "latitude", nullable = false, precision = 18, scale = 10)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 18, scale = 10)
    private BigDecimal longitude;

    public Region() {
    }

    public Region(String regionName, BigDecimal latitude, BigDecimal longitude) {
        this.regionName = regionName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Region of(String regionName, BigDecimal latitude, BigDecimal longitude) {
        return new Region(regionName, latitude, longitude);
    }

    public void updateRegion(String regionName, BigDecimal latitude, BigDecimal longitude) {
        this.regionName = regionName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getter
    public Long getId() {
        return id;
    }

    public String getRegionName() {
        return regionName;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    // Setter
    public void setId(Long id) {
        this.id = id;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
