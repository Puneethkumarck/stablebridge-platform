package com.stablecoin.payments.merchant.onboarding.infrastructure.temporal.activity;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface KafkaEventActivities {

  void publishEvent(String topic, String key, String payload);
}
