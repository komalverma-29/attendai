package com.attendai.core.face.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock (no-op) implementation of {@link FaceRecognitionEngine}.
 *
 * <p>Active by default when no other {@link FaceRecognitionEngine} bean is present.
 * In production, replace this with a real engine implementation and annotate it
 * with {@code @Primary} or configure it via {@code @ConditionalOnProperty}.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>{@code extractEmbedding} — returns an empty float array (no real computation).</li>
 *   <li>{@code findBestMatch} — always returns a no-match result with confidence 0.0.</li>
 *   <li>{@code isLive} — always returns {@code true} (liveness not enforced).</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "realFaceRecognitionEngine")
public class MockFaceRecognitionEngine implements FaceRecognitionEngine {

    @Override
    public FaceEmbedding extractEmbedding(byte[] imageBytes) {
        log.debug("MockFaceRecognitionEngine.extractEmbedding — returning empty vector");
        return FaceEmbedding.builder()
                .vector(new float[0])
                .build();
    }

    @Override
    public RecognitionResult findBestMatch(byte[] imageBytes, List<FaceEmbedding> candidates) {
        log.debug("MockFaceRecognitionEngine.findBestMatch — returning no-match");
        return RecognitionResult.builder()
                .matched(false)
                .confidence(0.0f)
                .build();
    }

    @Override
    public boolean isLive(byte[] imageBytes) {
        log.debug("MockFaceRecognitionEngine.isLive — returning true (mock)");
        return true;
    }
}
