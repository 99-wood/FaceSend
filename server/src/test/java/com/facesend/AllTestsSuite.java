package com.facesend;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        RoomControllerIntegrationTest.class,
        RoomServiceUnitTest.class,
        FileControllerIntegrationTest.class
})
public class AllTestsSuite {
}
