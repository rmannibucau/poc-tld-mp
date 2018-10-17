package org.talend.sdk.component.marketplace.service.ui;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(TYPE)
@Retention(RUNTIME)
public @interface View {
    /**
     * @return the model to use (a.k.a. "view") instead of the processed class.
     */
    Class<?> value();

    /**
     * Customize the generated uiSchema.
     */
    @Target({ FIELD, TYPE })
    @Retention(RUNTIME)
    @interface Schema {
        /**
         * @return the title to use instead of the default one (field name).
         */
        String title() default "";

        /**
         * @return use a relationship to generate a datalist.
         */
        Class<?> reference() default Schema.class;

        /**
         * @return type of the uiSchema.
         */
        String type() default "";

        /**
         * @return widget of the uiSchema.
         */
        String widget() default "";

        /**
         * @return is the field readOnly.
         */
        boolean readOnly() default false;

        /**
         * @return the order of fields in the uischema, is used for nested fields.
         */
        String[] order() default {};

        /**
         * IMPORTANT: if order and position are in use, order wins.
         *
         * @return the position of this fields, this is an alternative configuration to order.
         */
        int position() default -1;

        /**
         * @return the pattern to validate if set.
         */
        String pattern() default "";

        /**
         * @return the field length (for strings).
         */
        int length() default -1;

        /**
         * @return is the field required.
         */
        boolean required() default false;
    }

    /**
     * Mark a field as ignored.
     */
    @Target(FIELD)
    @Retention(RUNTIME)
    @interface Skip {
    }
}
