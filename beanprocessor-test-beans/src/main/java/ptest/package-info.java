@Tuples(20)
@Specialize({
    @Tuple(tupleTypeName="AllPrims", value={byte.class, short.class, int.class, long.class, char.class, float.class, double.class}),
    @Tuple(value={byte.class, short.class, int.class, long.class, char.class, float.class}),
    @Tuple(tupleTypeName="DoubleString", value={double.class, String.class})
    })
package ptest;
import com.soletta.beanprocessor.Specialize;
import com.soletta.beanprocessor.Tuple;
import com.soletta.beanprocessor.Tuples;


 