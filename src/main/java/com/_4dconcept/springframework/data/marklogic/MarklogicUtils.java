package com._4dconcept.springframework.data.marklogic;

import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentEntity;
import com._4dconcept.springframework.data.marklogic.core.mapping.MarklogicPersistentProperty;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Helper class featuring helper methods for working with Marklogic specific elements.
 * Mainly intended for internal use within the framework.
 *
 * @author stoussaint
 * @since 2017-11-30
 */
public final class MarklogicUtils {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /**
     * Private constructor to prevent instantiation.
     */
    private MarklogicUtils() {}

    /**
     * Expands the given expression using the provided type as context.
     *
     * @param expression the expression to expand
     * @param entityType the entityType used as context
     * @return the expanded expression. If the given expression is a literal or null, it is return as it.
     */
    @Nullable
    public static String expandsExpression(@Nullable String expression, @Nullable Class<?> entityType) {
        return expandsExpression(expression, entityType, null, null);
    }

    /**
     * Expands the given expression using the provided type, entity and id as context.
     *
     * @param expression the expression to expand
     * @param entityType the entityType used as context
     * @param entity the entity to use as context
     * @return the expanded expression. If the given expression is a literal or null, it is return as it.
     */
    @Nullable
    public static String expandsExpression(@Nullable String expression, @Nullable Class<?> entityType, @Nullable Object entity, @Nullable Supplier<Object> idSupplier) {
        return expandsExpression(expression, new DocumentExpressionContext() {
            @Override
            public Class<?> getEntityClass() {
                return entityType;
            }

            @Override
            public Object getEntity() {
                return entity;
            }

            @Override
            public Object getId() {
                return idSupplier != null ? idSupplier.get() : null;
            }
        });
    }

    @Nullable
    public static Object retrieveIdentifier(Object object, MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        MarklogicPersistentProperty idProperty = getIdPropertyFor(object.getClass(), mappingContext);

        if (idProperty == null) {
            throw new InvalidDataAccessApiUsageException("Unable to retrieve expected identifier property !");
        }

        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(object.getClass());

        if (persistentEntity == null) {
            throw new TypeMismatchDataAccessException(String.format("No Persistent Entity information found for the class %s", object.getClass()));
        }

        return persistentEntity.getPropertyAccessor(object).getProperty(idProperty);
    }

    @Nullable
    public static MarklogicPersistentProperty getIdPropertyFor(Class<?> entityType, MappingContext<? extends MarklogicPersistentEntity<?>, MarklogicPersistentProperty> mappingContext) {
        MarklogicPersistentEntity<?> persistentEntity = mappingContext.getPersistentEntity(entityType);
        return persistentEntity == null ? null : persistentEntity.getIdProperty();
    }

    public static <E extends MarklogicPersistentEntity<?>> E retrievePersistentEntity(Class<?> aClass, MappingContext<E, MarklogicPersistentProperty> mappingContext) {
        E persistentEntity = mappingContext.getPersistentEntity(aClass);

        if (persistentEntity == null) {
            throw new TypeMismatchDataAccessException(String.format("No Persistent Entity information found for the class %s", aClass));
        }

        return persistentEntity;
    }

    /**
     * Expands the given expression using the provided context.
     *
     * @param expression the collection to expand
     * @param context the context to use during resolution
     * @return the expanded expression. If the given expression is a literal or null, it is return as it.
     */
    @Nullable
    private static String expandsExpression(@Nullable String expression, Object context) {
        Expression spelExpression = detectExpression(expression);
        return spelExpression == null ? expression : spelExpression.getValue(context, String.class);
    }

    /**
     * Returns a SpEL {@link Expression} for the uri pattern expressed if present or {@literal null} otherwise.
     * Will also return {@literal null} if the uri pattern {@link String} evaluates
     * to a {@link LiteralExpression} (indicating that no subsequent evaluation is necessary).
     *
     * @param urlPattern can be {@literal null}
     * @return the dynamic Expression if any or {@literal null}
     */
    @Nullable
    private static Expression detectExpression(@Nullable String urlPattern) {
        if (!StringUtils.hasText(urlPattern)) {
            return null;
        }

        Expression expression = PARSER.parseExpression(urlPattern, ParserContext.TEMPLATE_EXPRESSION);

        return expression instanceof LiteralExpression ? null : expression;
    }

    private interface DocumentExpressionContext {
        @Nullable
        Class<?> getEntityClass();

        @Nullable
        Object getEntity();

        @Nullable
        Object getId();
    }

}
