# AWS S3 Log Listening Test Setup

Use this checklist to create a real AWS-based test environment for validating S3 log listening.

## Goal

- [ ] Create an S3 bucket to store test log files.
- [ ] Create an EC2 instance that generates logs.
- [ ] Configure IAM permissions for:
    - [ ] Local application read access to S3.
    - [ ] EC2 write access to S3.
- [ ] Upload continuously changing logs to S3.
- [ ] Open/listen to those S3 logs from the application.
- [ ] Validate object updates, prefix monitoring, and deletion detection.

---

## 1. Create an S3 Bucket

- [x] Open the AWS Console.
- [x] Go to **S3**.
- [x] Click **Create bucket**.
- [x] Choose a globally unique bucket name, for example:
```
text klogviewer-test-logs-yourname
``` 

- [ ] Choose the AWS region you want to use, for example:

```
text us-east-1
``` 

or:
```
text eu-west-1
``` 

- [x] Keep **Block all public access** enabled.
- [x] Create the bucket.
- [x] Use this logical object layout for testing:

```
text logs/ app.log app-2.log
``` 

> You do not need to manually create folders in S3. The `logs/` prefix will exist once you upload an object like `logs/app.log`.

---

## 2. Create an IAM Policy for Local S3 Read Access

This policy allows your local machine/application to browse the bucket and read log objects.

- [x] Go to **IAM > Policies**.
- [x] Click **Create policy**.
- [x] Choose the **JSON** editor.
- [x] Paste this policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ListTestLogBucket",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::klogviewer-test-logs-yourname"
    },
    {
      "Sid": "ReadTestLogObjects",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::klogviewer-test-logs-yourname/*"
    }
  ]
}
``` 

- [x] Replace all occurrences of:
```text 
klogviewer-test-logs-yourname
``` 

with your real bucket name.

- [x] Name the policy:
```text 
KLogViewerS3ReadOnlyTestPolicy
``` 

- [x] Create the policy.

---

## 3. Create an IAM User for Local Testing

- [x] Go to **IAM > Users**.
- [x] Click **Create user**.
- [x] Name the user:
```text 
klogviewer-s3-test-user
``` 

- [x] Attach the policy:
- 
```text 
KLogViewerS3ReadOnlyTestPolicy
``` 

- [x] Create an access key for CLI/application access.
- [x] Save the generated credentials:
    - [x] `AWS_ACCESS_KEY_ID`
    - [x] `AWS_SECRET_ACCESS_KEY`

1. Go to the AWS Console.
2. Open IAM.
3. Go to Users.
4. Select the IAM user you created for testing, for example:
   ``` text
    klogviewer-s3-test-user
    ```
5. Open the **Security credentials** tab.
6. Scroll to **Access keys**.
7. Click **Create access key**. 
8. For the use case, choose:
    ``` text
   Command Line Interface (CLI)
    ```
9.AWS may show a recommendation to use alternatives. Confirm that you understand and continue. 
10.Optionally add a description tag, for example:
    ``` text
    KLogViewer local S3 testing
    ```
11, Click **Create access key**. 
12. Copy or download the credentials.
    You will get two values:
    ``` text
        AWS_ACCESS_KEY_ID
        AWS_SECRET_ACCESS_KEY
    ```
Important: AWS only shows the **secret access key once**. Download the .csv file or copy it somewhere secure 
immediately.

---

## 4. Configure a Local AWS Profile

- [x] Install the AWS CLI locally if you do not already have it.
- [x] Configure a dedicated test profile:
```bash 
aws configure --profile klogviewer-test
``` 

- [x] Enter the access key ID.
- [x] Enter the secret access key.
- [x] Enter the default region (**check the region you created the bucket in**), for example:
```text 
eu-north-1
``` 

- [x] Enter the default output format:
``` text
json
``` 

- [x] Verify the profile can access the bucket (**change `yourname` to the name you chose for the bucket**):
```bash 
aws s3 ls s3://klogviewer-test-logs-yourname --profile klogviewer-test
``` 

- [x] Confirm the command succeeds without an access denied error.

---

## 5. Create an EC2 Instance

- [ ] Go to **EC2 > Instances**.
- [ ] Click **Launch instance**.
- [ ] Name the instance:
```text 
klogviewer-log-generator
``` 

- [ ] Choose an AMI:
```text 
Amazon Linux 2023
``` 

- [ ] Choose an instance type:
```text 
t3.micro
``` 

or, if free-tier eligible:
```text 
t2.micro
``` 

- [x] Create or select an SSH key pair (**when saving the key, make sure to set the correct permissions**):
    - [x] Save the key pair as a .pem file.
    - [x] Set the permissions: `chmod 400 /path/to/your-key.pem`.
- [x] Configure the security group:
    - [x] Allow SSH.
    - [x] Restrict SSH access to your IP address only.
- [x] Launch the instance.

---

## 6. Create an IAM Policy for EC2 S3 Write Access

The EC2 instance needs permission to upload log files to your S3 bucket.

- [x] Go to **IAM > Policies**.
- [x] Click **Create policy**.
- [x] Choose the **JSON** editor.
- [x] Paste this policy:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "WriteLogsToTestBucket",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::klogviewer-test-logs-kevin",
        "arn:aws:s3:::klogviewer-test-logs-kevin/*"
      ]
    }
  ]
}
``` 

- [x] Replace all occurrences of:
```text 
klogviewer-test-logs-yourname
``` 

with your real bucket name.

- [x] Name the policy:
```text 
KLogViewerS3WriteTestPolicy
``` 

- [x] Create the policy.

---

## 7. Create and Attach an EC2 IAM Role

- [x] Go to **IAM > Roles**.
- [x] Click **Create role**.
- [x] Choose **AWS service** as the trusted entity type.
- [x] Choose **EC2** as the use case.
- [x] Attach this policy:

```text 
KLogViewerS3WriteTestPolicy
``` 

- [x] Name the role:

```text 
KLogViewerLogGeneratorRole
``` 

- [x] Create the role.
- [x] Go to **EC2 > Instances**.
- [x] Select your test instance.
- [x] Choose **Actions > Security > Modify IAM role**.
- [x] Select:
```text 
KLogViewerLogGeneratorRole
``` 

- [x] Save the change.

---

## 8. SSH Into the EC2 Instance

- [x] Get the public IP or public DNS name of the EC2 instance.
- [x] SSH into the instance:
```bash
ssh -i /path/to/your-key.pem ec2-user@your-ec2-public-ip
``` 

- [x] Confirm you are connected to the EC2 instance.

---

## 9. Verify AWS CLI on EC2

- [x] Check whether AWS CLI is installed:
```bash
aws --version
``` 

- [x] If it is not installed, install it:
```bash
sudo dnf install -y awscli
``` 

- [x] Verify that the EC2 instance role can access the bucket:
```bash
aws s3 ls s3://klogviewer-test-logs-yourname
``` 

- [x] Confirm the command succeeds without configuring credentials manually on the EC2 instance.

---

## 10. Create a Log Generator on EC2

- [x] Create a working directory:
```bash 
mkdir -p ~/klogviewer-test 
cd ~/klogviewer-test 
touch app.log
``` 

- [x] Create a script:
```bash 
nano generate-and-upload-logs.sh
``` 

- [x] Add this content:
```bash
 #!/usr/bin/env bash

BUCKET="klogviewer-test-logs-kevin"
KEY="logs/app.log"
LOCAL_FILE="./app.log"

echo "Starting log generator..."
echo "Writing to ${LOCAL_FILE}"
echo "Uploading to s3://${BUCKET}/${KEY}"

while true; do
  echo "$(date --iso-8601=seconds) INFO Test log message from EC2 instance $(hostname)" >> "${LOCAL_FILE}"

  aws s3 cp "${LOCAL_FILE}" "s3://${BUCKET}/${KEY}" >/dev/null

  sleep 3
done
``` 

- [x] Replace:
```text 
klogviewer-test-logs-yourname
``` 

with your real bucket name.

- [x] Make the script executable:
```bash 
chmod +x generate-and-upload-logs.sh
``` 

- [x] Run the script:
```bash 
./generate-and-upload-logs.sh
``` 

- [x] Leave it running.

---

## 11. Verify the S3 Object Is Updating

From your local machine:

- [x] Download the current object content to stdout (**remember to change 'yourname' to your bucket name**:
```bash 
aws s3 cp s3://klogviewer-test-logs-yourname/logs/app.log - --profile klogviewer-test
``` 

- [x] Confirm you see log lines similar to:
```text 
2026-05-23T12:01:01+00:00 INFO Test log message from EC2 instance ip-10-0-1-123 2026-05-23T12:01:04+00:00 INFO Test log message from EC2 instance ip-10-0-1-123 2026-05-23T12:01:07+00:00 INFO Test log message from EC2 instance ip-10-0-1-123
``` 

- [x] Wait a few seconds.
- [x] Run the same command again.
- [x] Confirm new lines have appeared.

---

## 12. Connect the Application to S3

- [ ] Open the application.
- [ ] Start the S3 connection flow.
- [ ] Use the local AWS profile:
```text 
klogviewer-test
``` 

- [ ] Use the same AWS region as the bucket, for example:
```text 
eu-north-1
``` 

- [ ] Use your bucket name:
```text 
klogviewer-test-logs-yourname
``` 

- [ ] Select or enter the object key:
```text 
logs/app.log
``` 

- [x] Open the S3 log source.
- [x] Confirm the initial log content appears in the application.

---

## 13. Test S3 Tailing / Polling Behavior

- [x] Keep the EC2 log generator script running.
- [x] Keep the S3 log open in the application.
- [x] Wait for the application to poll S3.
- [x] Confirm new log lines appear in the viewer.
- [x] Remember that S3 does not provide true streaming like `tail -f`; updates usually appear after the polling 
  interval.

Expected flow:

- [x] EC2 appends a new line to `app.log`.
- [x] EC2 uploads the updated file to S3.
- [x] The application polls S3.
- [x] The application detects the larger object.
- [x] The application displays the appended log lines.

---

## 14. Test S3 Prefix Monitoring

Use this if the application supports watching an S3 prefix such as `logs/`.

- [ ] In the application, open or monitor the prefix:
```text 
logs/
``` 

- [ ] On EC2, create another log file:
```bash 
cd ~/klogviewer-test touch app-2.log
``` 

- [ ] Upload the new file:
```bash 
aws s3 cp app-2.log s3://klogviewer-test-logs-yourname/logs/app-2.log
``` 

- [ ] Append a line to the second log file:
```bash 
echo "$(date --iso-8601=seconds) WARN Message from app-2" >> app-2.log
``` 

- [ ] Upload the updated second log file:
```bash 
aws s3 cp app-2.log s3://klogviewer-test-logs-yourname/logs/app-2.log
``` 

- [ ] Confirm the application detects the new object:
```text 
logs/app-2.log
``` 

- [ ] Confirm the application displays log lines from the new object.

---

## 15. Test Deletion Detection

Use this if the application supports deletion detection for monitored S3 prefixes.

- [ ] Delete the second test object:
```bash 
aws s3 rm s3://klogviewer-test-logs-yourname/logs/app-2.log
``` 

- [ ] Wait for the application to rescan/poll the prefix.
- [ ] Confirm the deleted object is handled as expected.
- [ ] Confirm the UI either:
    - [ ] Marks the object as missing.
    - [ ] Removes the object from the monitored prefix view.
    - [ ] Shows the expected warning state.

---

## 16. Optional: Test Without EC2

Use this faster local-only flow if you only want to test S3 polling and do not need an EC2-hosted log generator.

- [ ] Create a local test directory:
```bash 
mkdir -p /tmp/klogviewer-test cd /tmp/klogviewer-test touch app.log
``` 

- [ ] Start a local upload loop:
```bash 
while true; do echo "$(date --iso-8601=seconds) INFO Local test log message" >> app.log aws s3 cp app.log s3://klogviewer-test-logs-yourname/logs/app.log --profile klogviewer-test sleep 3 done
``` 

- [ ] Open the same S3 object in the application:
```text 
logs/app.log
``` 

- [ ] Confirm new lines appear after each upload/poll cycle.

---

## 17. Troubleshooting Checklist

### Access denied locally

- [ ] Confirm the local profile exists:
```bash 
aws configure list --profile klogviewer-test
``` 

- [ ] Confirm the profile can list the bucket:
```bash 
aws s3 ls s3://klogviewer-test-logs-yourname --profile klogviewer-test
``` 

- [ ] Confirm the IAM user has:
    - [ ] `s3:ListBucket`
    - [ ] `s3:GetObject`

### EC2 cannot upload to S3

- [ ] Confirm the IAM role is attached to the EC2 instance.
- [ ] Confirm the role policy allows:
    - [ ] `s3:PutObject`
    - [ ] `s3:GetObject`
    - [ ] `s3:ListBucket`
- [ ] Run this on EC2:
```bash 
aws s3 ls s3://klogviewer-test-logs-yourname
``` 

- [ ] Try a manual upload from EC2:
```bash 
echo "test" > test.log aws s3 cp test.log s3://klogviewer-test-logs-yourname/logs/test.log
``` 

### Application does not show new lines

- [ ] Confirm the S3 object is actually changing:
```bash 
aws s3 cp s3://klogviewer-test-logs-yourname/logs/app.log - --profile klogviewer-test
``` 

- [ ] Confirm the application is using the correct:
    - [ ] AWS profile.
    - [ ] Region.
    - [ ] Bucket name.
    - [ ] Object key or prefix.
- [ ] Wait longer than the configured polling interval.
- [ ] Confirm the object is growing rather than being truncated unexpectedly.

### Application cannot browse the bucket

- [ ] Confirm `s3:ListBucket` is granted on the bucket ARN:
```text 
arn:aws:s3:::klogviewer-test-logs-yourname
``` 

- [ ] Confirm `s3:GetObject` is granted on the object ARN pattern:
```text 
arn:aws:s3:::klogviewer-test-logs-yourname/*
``` 

---

## 18. Cleanup

When testing is complete, clean up resources to avoid unnecessary AWS charges.

### Stop the EC2 generator

- [ ] Press `Ctrl+C` in the EC2 SSH session running the script.

### Terminate EC2

- [ ] Go to **EC2 > Instances**.
- [ ] Select:
```text 
klogviewer-log-generator
``` 

- [ ] Stop or terminate the instance.

### Delete S3 objects

- [ ] Remove the test logs:
```bash 
aws s3 rm s3://klogviewer-test-logs-yourname/logs/ --recursive --profile klogviewer-test
``` 

### Delete the S3 bucket

- [ ] Delete the bucket:
```bash 
aws s3 rb s3://klogviewer-test-logs-yourname --profile klogviewer-test
``` 

If the bucket is not empty, use:
```bash 
aws s3 rb s3://klogviewer-test-logs-yourname --force --profile klogviewer-test
``` 

### Remove IAM test resources

- [ ] Delete or deactivate the IAM user:
```text 
klogviewer-s3-test-user
``` 

- [ ] Delete the IAM policies if no longer needed:
    - [ ] `KLogViewerS3ReadOnlyTestPolicy`
    - [ ] `KLogViewerS3WriteTestPolicy`
- [ ] Delete the EC2 IAM role if no longer needed:
```text 
KLogViewerLogGeneratorRole
``` 

---

## 19. Final Validation Checklist

- [ ] S3 bucket exists.
- [ ] Local AWS profile can list the bucket.
- [ ] Local AWS profile can read objects.
- [ ] EC2 instance exists.
- [ ] EC2 instance has an IAM role attached.
- [ ] EC2 instance can upload to S3.
- [ ] `logs/app.log` exists in S3.
- [ ] `logs/app.log` grows over time.
- [ ] Application can connect to S3.
- [ ] Application can open `logs/app.log`.
- [ ] Application displays initial S3 log content.
- [ ] Application receives/appends new lines after polling.
- [ ] Application can monitor `logs/` prefix if supported.
- [ ] Application detects new objects under `logs/` if supported.
- [ ] Application handles deleted objects if supported.
- [ ] EC2 and S3 resources are cleaned up after testing.
```

.
