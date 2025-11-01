package com.github.ahuemmer.storesthal.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ahuemmer.storesthal.StoresthalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration.EMBEDDED_PARENT_NAME;

/**
 * This helper class is used by Storesthal to be able to retrieve embedded collections (collections which are supplied
 * under an {@code _embedded} object, as it is a good practice in HATEOAS). The crucial point that makes it tricky is,
 * that the object name of the array object containing the collection (which is a JSON child object of the
 * {@code _embedded} node), is not known to Storesthal at compile time. Therefore, we're working with
 * {@link com.fasterxml.jackson.databind.JsonNode}s and {@link com.fasterxml.jackson.databind.JavaType} here.
 *
 * @param <T>
 */
public class EmbeddedCollectionHelper<T> {

    /**
     * The logger, mainly for logging warnings if nothing was found.
     */
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedCollectionHelper.class);

    /**
     * The actual object holding the collection array as child of the {@code _embedded} node.
     */
    @JsonProperty(EMBEDDED_PARENT_NAME)
    private JsonNode objectCollection;
    //private Collection<T> objectCollection;

    /**
     * Extract the actual collection from the JSON response.
     * <p>
     * This is the tricky part: While <i>retrieving</i> the response using the standard Jackson mechanisms does not need
     * to know about the type ob objects stored in the collection (as we're just using a
     * {@link com.fasterxml.jackson.databind.JsonNode} here), this type has to be known when extracting the actual
     * collection objects afterward. In order o achieve this, this static function was created.
     *
     * @param response    The response of the request, which indeed is an instance of {@code EmbeddedCollectionHelper}
     *                    itself.
     * @param objectClass The final class of the objects contained within the collection. (Will be wrapped within
     *                    {@link org.springframework.hateoas.EntityModel}.)
     * @param fieldName   The name of the field as child of {@code _embedded} in the JSON structure. If the
     *                    {@code Optional} is empty, the first field encountered and containing an array is used.
     * @param <T>         The type of the objects in the collections (meaning the type of {@code objectClass}).
     * @return A list containing the collection objects as instances of {@code EntityModel<T>}. If no matching field was
     * found as child of the {@code _embedded} JSON object (either no field with the given name was found, if
     * {@code fieldName} was not empty, or no array-type field has been found under {@code _embedded} else).
     * @throws StoresthalException
     */
    public static <T> List<EntityModel<T>> getObjects(ResponseEntity response, Class<T> objectClass, Optional<String> fieldName) throws StoresthalException, IOException {

        EmbeddedCollectionHelper<T> helper = (EmbeddedCollectionHelper<T>) response.getBody();

        String fieldNameFound = null;

        Iterator<String> fieldNameIterator = helper.getObjectCollection().fieldNames();

        if (fieldName.isEmpty()) {
            if (fieldNameIterator.hasNext()) {
                String currentFieldName = fieldNameIterator.next();
                if (helper.getObjectCollection().get(currentFieldName).isArray()) {
                    fieldNameFound = currentFieldName;
                }
            }
        } else {
            while (fieldNameIterator.hasNext()) {
                String currentFieldName = fieldNameIterator.next();
                if (fieldName.get().equals(currentFieldName)) {
                    fieldNameFound = currentFieldName;
                    if (!helper.getObjectCollection().get(fieldNameFound).isArray()) {
                        throw new StoresthalException("Embedded collection is not an array in field " + fieldName);
                    }
                    break;
                }
            }
        }

        if (fieldNameFound == null) {
            if (fieldName.isPresent()) {
                logger.warn("Field \"{}\" not found in embedded collection. Returning empty result.", fieldName.get());
            } else {
                logger.warn("No array field found in embedded collection. Returning empty result.");
            }
            return new ArrayList<>();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());

        JavaType type = objectMapper.getTypeFactory().constructParametricType(EntityModel.class, objectClass);
        JavaType listType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, type);

        var reader = objectMapper.readerFor(listType);

        return reader.readValue(helper.getObjectCollection().get(fieldNameFound));
    }

    /**
     * Get the object collection.
     *
     * @return The "arbitrary" (as not-yet-typed) object collection, represented by the {@code _embedded} JSON node.
     */
    public JsonNode getObjectCollection() {
        return objectCollection;
    }

    /**
     * Set the object Collection root node (denoting the {@code _embedded} JSON node).
     *
     * @param objectCollection The {@code _embedded} JSON node.
     */
    public void setObjectCollection(JsonNode objectCollection) {
        this.objectCollection = objectCollection;
    }

}
