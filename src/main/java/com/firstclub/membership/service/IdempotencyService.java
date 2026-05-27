package com.firstclub.membership.service;

import com.firstclub.membership.domain.IdempotencyRecord;
import com.firstclub.membership.repository.IdempotencyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Application service that enforces at-least-once idempotency for write operations
 * such as {@code subscribe} and {@code checkout}.
 *
 * <p>The service stores the JSON-serialized response of an operation keyed by the
 * triplet {@code (userId, key, operation)}. A subsequent call with the same triplet
 * returns the persisted response instead of executing the action again, providing
 * the at-least-once guarantee described in the Concurrency Design section of the
 * design document.
 *
 * <p>This service intentionally does <strong>not</strong> declare
 * {@code @Transactional}; the caller dictates the transactional boundary so that
 * the action and the {@link IdempotencyRecord} insert participate in the same
 * unit of work.
 *
 * <p>Validates: Requirements 2.4, 8.3, 9.4.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Constructs the service with its collaborators.
     *
     * @param idempotencyRepository repository used to look up and persist records
     * @param objectMapper          Jackson mapper used to (de)serialize responses
     * @param clock                 clock used to stamp newly created records
     */
    public IdempotencyService(IdempotencyRepository idempotencyRepository,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Executes {@code action} at most once per {@code (userId, key, operation)}
     * triplet and returns its result.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If {@code key} is {@code null} or blank, the action is executed
     *       without persisting any idempotency record.</li>
     *   <li>If a record already exists for the triplet, the persisted response
     *       is deserialized into {@code resultType} and returned without
     *       invoking {@code action}.</li>
     *   <li>Otherwise, {@code action} is executed, the result is serialized to
     *       JSON, and a new {@link IdempotencyRecord} is persisted in the
     *       caller's transaction.</li>
     *   <li>If a concurrent inserter wins the race and the persist fails with
     *       {@link DataIntegrityViolationException}, the existing record is
     *       re-fetched and its deserialized response is returned, preserving
     *       at-least-once semantics.</li>
     * </ul>
     *
     * @param userId     owning user identifier
     * @param key        client-supplied idempotency key; {@code null} or blank
     *                   disables idempotency for this call
     * @param operation  logical operation name (for example, {@code "subscribe"}
     *                   or {@code "checkout"})
     * @param resultType class of the value returned by {@code action}; used to
     *                   deserialize cached responses
     * @param action     the operation to execute when no cached response exists
     * @param <T>        the result type
     * @return the action's result, or the previously persisted result for the
     *         same triplet
     */
    public <T> T runOnce(String userId,
                         @Nullable String key,
                         String operation,
                         Class<T> resultType,
                         Supplier<T> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }

        Optional<IdempotencyRecord> existing =
                idempotencyRepository.findByUserIdAndKeyAndOperation(userId, key, operation);
        if (existing.isPresent()) {
            return deserialize(existing.get().getResponseJson(), resultType);
        }

        T result = action.get();
        String responseJson = serialize(result);
        IdempotencyRecord record = new IdempotencyRecord(
                userId,
                key,
                operation,
                "",
                responseJson,
                clock.instant());
        try {
            idempotencyRepository.save(record);
        } catch (DataIntegrityViolationException concurrentInsert) {
            IdempotencyRecord raced = idempotencyRepository
                    .findByUserIdAndKeyAndOperation(userId, key, operation)
                    .orElseThrow(() -> concurrentInsert);
            return deserialize(raced.getResponseJson(), resultType);
        }
        return result;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize idempotency response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> resultType) {
        try {
            return objectMapper.readValue(json, resultType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize idempotency response", e);
        }
    }
}
