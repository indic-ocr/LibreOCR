package io.github.rkvsraman.libreocr.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import io.github.rkvsraman.libreocr.tests.base.UnoSuite;
import io.github.rkvsraman.libreocr.tests.uno.WriterTest;

@RunWith(UnoSuite.class)
@SuiteClasses({WriterTest.class})
public class UnoTests {

}
