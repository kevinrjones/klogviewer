Feature: Load log file
  As a user
  I want to load a log file
  So that I can see the log entries in the viewer

  Scenario: Loading a valid log file
    Given a log file exists at "test.log" with content:
      """
      2024-05-12 10:00:00 [INFO] First message
      2024-05-12 10:05:00 [ERROR] Second message
      """
    When I load the log file "test.log"
    Then I should see 2 log entries
    And the first entry should have level "INFO" and content "First message"
    And the second entry should have level "ERROR" and content "Second message"
