package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "patterns")
@Getter
@Setter
@NoArgsConstructor
public class Pattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "order_in_plan")
    private Integer orderInPlan;
}
