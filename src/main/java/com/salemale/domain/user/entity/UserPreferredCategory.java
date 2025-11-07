package com.salemale.domain.user.entity;

import com.salemale.global.common.BaseEntity;
import com.salemale.global.common.enums.Category;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferred_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_category",
                        columnNames = {"user_id", "category"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserPreferredCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private Category category;
}