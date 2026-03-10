package com.example.breeze_seas;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Test-only helper that restores {@link TicketsRepository} to its seeded demo state.
 *
 * <p>The repository is a mutable singleton, so instrumented tests need a deterministic reset
 * point between runs without widening production API surface purely for tests.
 */
public final class TicketsRepositoryTestUtils {

    private TicketsRepositoryTestUtils() {
    }

    public static void resetDemoData() {
        try {
            TicketsRepository repository = TicketsRepository.getInstance();

            clearListField(repository, "listeners");
            clearListField(repository, "activeTickets");
            clearListField(repository, "attendingTickets");
            clearListField(repository, "pastTickets");

            Method seedDemoData = TicketsRepository.class.getDeclaredMethod("seedDemoData");
            seedDemoData.setAccessible(true);
            seedDemoData.invoke(repository);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to reset TicketsRepository demo data for tests", e);
        }
    }

    private static void clearListField(TicketsRepository repository, String fieldName)
            throws ReflectiveOperationException {
        Field field = TicketsRepository.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        List<?> list = (List<?>) field.get(repository);
        list.clear();
    }
}
