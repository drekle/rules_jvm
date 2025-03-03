package com.github.bazel_contrib.contrib_rules_jvm.junit5;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamWriter;
import org.junit.platform.launcher.TestIdentifier;

class TestSuiteResult extends BaseResult {

  private final List<TestResult> results = new LinkedList<>();
  private final List<TestSuiteResult> nestedSuites = new LinkedList<>();

  public TestSuiteResult(TestIdentifier id) {
    super(id);
  }

  @Override
  public void toXml(XMLStreamWriter xml) {
    write(
        () -> {
          xml.writeStartElement("testsuite");

          xml.writeAttribute("name", getTestId().getLegacyReportingName());
          xml.writeAttribute("tests", String.valueOf(results.size()));

          int errors = 0;
          int failures = 0;
          int disabled = 0;
          int skipped = 0;
          for (TestResult result : results) {
            if (result.isError()) {
              errors++;
            }
            if (result.isFailure()) {
              failures++;
            }
            if (result.isDisabled()) {
              disabled++;
            }
            if (result.isSkipped()) {
              skipped++;
            }
          }
          xml.writeAttribute("failures", String.valueOf(failures));
          xml.writeAttribute("errors", String.valueOf(errors));
          xml.writeAttribute("disabled", String.valueOf(disabled));
          xml.writeAttribute("skipped", String.valueOf(skipped));

          // The bazel junit4 test runner seems to leave these values empty.
          // Emulating that somewhat strange behaviour here.
          xml.writeAttribute("package", "");
          xml.writeEmptyElement("properties");

          results.forEach(res -> res.toXml(xml));
          nestedSuites.forEach(suite -> suite.toXml(xml));

          String stdout = getStdOut();
          if (stdout != null) {
            xml.writeStartElement("system-out");
            xml.writeCData(stdout);
            xml.writeEndElement();
          }

          String stderr = getStdErr();
          if (stderr != null) {
            xml.writeStartElement("system-err");
            xml.writeCData(stderr);
            xml.writeEndElement();
          }

          xml.writeEndElement();
        });
  }

  public void add(TestResult testResult) {
    results.add(testResult);
  }

  public void add(TestSuiteResult suite) {
    nestedSuites.add(suite);
  }

  @Override
  protected Optional<BaseResult> get(TestIdentifier id) {
    if (getTestId().equals(id)) {
      return Optional.of(this);
    }

    return Stream.concat(results.stream(), nestedSuites.stream())
        .map(result -> result.get(id))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
}
