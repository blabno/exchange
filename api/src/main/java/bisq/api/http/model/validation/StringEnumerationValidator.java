package bisq.api.http.model.validation;

import java.util.Set;
import java.util.TreeSet;



import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

public class StringEnumerationValidator implements ConstraintValidator<StringEnumeration, String> {

    private Set<String> availableValues;

    @Override
    public void initialize(StringEnumeration constraintAnnotation) {
        availableValues = new TreeSet<>();
        Enum<?>[] enums = constraintAnnotation.enumClass().getEnumConstants();
        for (Enum<?> anEnum : enums)
            availableValues.add(anEnum.name());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        boolean valid = value == null || availableValues.contains(value);
        if (!valid) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.addExpressionVariable("availableValues", getAvailableValuesAsString());
        }
        return valid;
    }

    private String getAvailableValuesAsString() {
        StringBuilder builder = new StringBuilder();
        for (String item : availableValues) {
            if (0 < builder.length()) builder.append(", ");
            builder.append(item);
        }
        return builder.toString();
    }
}
