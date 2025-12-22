package org.commons.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for unit tests.
 * Provides Mockito support for all unit tests.
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseUnitTest {
    // Base class for unit tests - MockitoExtension provides @Mock and @InjectMocks support
}
