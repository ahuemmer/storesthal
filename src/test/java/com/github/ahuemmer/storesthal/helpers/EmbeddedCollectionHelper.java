package com.github.ahuemmer.storesthal.helpers;

import com.github.ahuemmer.storesthal.StoresthalException;
import com.github.ahuemmer.storesthal.complextestobjects.ObjectCollection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddedCollectionHelperTest {

    @Mock
    private ResponseEntity responseEntity;

    @Test
    @DisplayName("returns empty list on missing collection")
    void returns_empty_list_on_missing_collection() throws StoresthalException {
        when(responseEntity.getBody()).thenReturn(new EmbeddedCollectionHelper<ObjectCollection>());
        assertTrue(EmbeddedCollectionHelper.getObjects(responseEntity, ObjectCollection.class, Optional.empty()).isEmpty());
    }

    @Test
    @DisplayName("returns empty list on missing EmbeddedCollectionHelper")
    void returns_empty_list_on_missing_EmbeddedCollectionHelper() throws StoresthalException {
        when(responseEntity.getBody()).thenReturn(null);
        assertTrue(EmbeddedCollectionHelper.getObjects(responseEntity, ObjectCollection.class, Optional.empty()).isEmpty());
    }

}
