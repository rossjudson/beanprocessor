package ptest;

public class DelegationTestImpl implements DelegationTest {

    @Override
    public void doSomething(int hello) {
    }

    @Override
    public Integer doSomething(double fix) {
        return 22;
    }

    @Override
    public boolean doSomething(String withThis) {
        return true;
    }

}
