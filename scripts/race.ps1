param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("Place", "Cancel", "Patch")]
    [string]$Mode,

    [int]$Count = 10,

    [int]$ProductId,

    [int]$OrderId,

    [string]$BaseUrl = "http://localhost:8080"
)

# Ensure System.Net.Http assembly is loaded in Windows PowerShell 5.1
Add-Type -AssemblyName System.Net.Http

# .NET Framework allows only 2 connections per host by default, which would serialize the requests
[System.Net.ServicePointManager]::DefaultConnectionLimit = 50

# Single shared HttpClient instance for sending all concurrent requests
$client = New-Object System.Net.Http.HttpClient
$client.BaseAddress = New-Object System.Uri($BaseUrl)

# Holds all in-flight asynchronous request tasks
$tasks = New-Object System.Collections.Generic.List[System.Threading.Tasks.Task[System.Net.Http.HttpResponseMessage]]

# Fire all requests at the exact same moment by queuing asynchronous tasks without awaiting
for ($i = 1; $i -le $Count; $i++) {
    $request = $null

    if ($Mode -eq "Place") {
        $json = "{`"customerName`":`"Racer $i`",`"items`":[{`"productId`":$ProductId,`"quantity`":1}]}"
        $content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, "/orders")
        $request.Content = $content
    }
    elseif ($Mode -eq "Cancel") {
        $request = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Delete, "/orders/$OrderId")
    }
    elseif ($Mode -eq "Patch") {
        $json = "{`"status`":`"SHIPPED`"}"
        $content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")
        $patchMethod = New-Object System.Net.Http.HttpMethod("PATCH")
        $request = New-Object System.Net.Http.HttpRequestMessage($patchMethod, "/orders/$OrderId/status")
        $request.Content = $content
    }

    # Queue request task without awaiting so all requests start together
    $task = $client.SendAsync($request)
    $tasks.Add($task)
}

# Block until all concurrent tasks complete
[System.Threading.Tasks.Task]::WaitAll($tasks.ToArray())

$statusCounts = @{}

# Process each response and output status code and message
foreach ($task in $tasks) {
    $response = $task.Result
    $statusCode = [int]$response.StatusCode
    $body = $response.Content.ReadAsStringAsync().Result

    $message = ""
    if (-not [string]::IsNullOrEmpty($body)) {
        try {
            $jsonObj = $body | ConvertFrom-Json
            if ($jsonObj.message) {
                $message = $jsonObj.message
            }
        }
        catch {
            # Plain body without JSON
        }
    }

    if ($message) {
        Write-Host "$statusCode $message"
    }
    else {
        Write-Host "$statusCode"
    }

    $key = "$statusCode"
    if ($statusCounts.ContainsKey($key)) {
        $statusCounts[$key] = $statusCounts[$key] + 1
    }
    else {
        $statusCounts[$key] = 1
    }
}

# Output count summary per status code
Write-Host "`n--- Summary ---"
foreach ($key in ($statusCounts.Keys | Sort-Object)) {
    Write-Host "$key : $($statusCounts[$key])"
}

$client.Dispose()
