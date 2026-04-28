param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 5678,
    [string]$BasePath = "/webhook",
    [string]$AuthToken = ""
)

$env:MOCK_N8N_HOST = $HostName
$env:MOCK_N8N_PORT = "$Port"
$env:MOCK_N8N_BASE_PATH = $BasePath

if ($AuthToken) {
    $env:MOCK_N8N_AUTH_TOKEN = $AuthToken
}
else {
    Remove-Item Env:MOCK_N8N_AUTH_TOKEN -ErrorAction SilentlyContinue
}

python "$PSScriptRoot\server.py"
