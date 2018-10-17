package org.talend.sdk.component.marketplace.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

@InterceptorBinding
@Target({ TYPE, METHOD })
@Retention(RUNTIME)
public @interface Tx {

    @Tx
    @Interceptor
    @Priority(Interceptor.Priority.LIBRARY_BEFORE)
    class Impl implements Serializable {
        @Inject
        private EntityManager em;

        @AroundInvoke
        public Object tx(final InvocationContext ctx) throws Exception {
            final EntityTransaction transaction = em.getTransaction();
            final boolean active = transaction.isActive();
            if (!active) {
                transaction.begin();
            }
            try {
                final Object proceed = ctx.proceed();
                em.flush();
                if (!active) {
                    transaction.commit();
                }
                return proceed;
            } catch (final RuntimeException re) {
                transaction.rollback();
                throw re;
            }
        }
    }
}
