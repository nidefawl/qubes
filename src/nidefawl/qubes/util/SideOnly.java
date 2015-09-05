package nidefawl.qubes.util;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface SideOnly {
    public Side value();
}
