package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.entity.ReviewState;

public record UrgentReviewItem(Problem problem, ReviewState state, double urgency) {
}
