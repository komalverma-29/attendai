package com.attendai.core.face.service;

import java.util.List;

/**
 * Abstraction over the face recognition computation engine.
 *
 * <p>Concrete implementations (e.g., a local DeepFace model or AWS Rekognition)
 * are injected by Spring. Swapping implementations requires only a configuration
 * change — {@code FaceServiceImpl} depends only on this interface.
 *
 * <p>V1 default: {@link MockFaceRecognitionEngine} — returns a no-match result.
 * Replace with a real implementation for production face recognition.
 */
public interface FaceRecognitionEngine {

    /**
     * Extracts a face embedding vector from the given image bytes.
     *
     * @param imageBytes raw image binary (JPEG, PNG, etc.)
     * @return the extracted embedding
     * @throws com.attendai.core.common.exception.ExternalServiceException if the engine fails
     */
    FaceEmbedding extractEmbedding(byte[] imageBytes);

    /**
     * Finds the best-matching person from the candidate embeddings.
     *
     * <p>The engine computes similarity between the query embedding and each
     * candidate. Returns the candidate with the highest similarity.
     * The caller applies the confidence threshold.
     *
     * @param imageBytes raw image binary of the person to identify
     * @param candidates all stored embeddings to compare against
     * @return the best-match result (may have {@code matched=false} if no candidates)
     * @throws com.attendai.core.common.exception.ExternalServiceException if the engine fails
     */
    RecognitionResult findBestMatch(byte[] imageBytes, List<FaceEmbedding> candidates);

    /**
     * Performs a liveness check on the given image bytes.
     *
     * @param imageBytes raw image binary
     * @return true if a live person is detected; false for spoofed images
     * @throws com.attendai.core.common.exception.ExternalServiceException if the engine fails
     */
    boolean isLive(byte[] imageBytes);
}
