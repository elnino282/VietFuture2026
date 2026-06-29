$controllerDir = "c:\Users\ACER\Documents\GitHub\SE122-Code-MicroserviceReady-Test\agricultural-crop-management-backend\src\main\java\org\example\QuanLyMuaVu\module\marketplace\controller"

Get-ChildItem -Path "$controllerDir\*.java" | ForEach-Object {
    $filePath = $_.FullName
    $content = Get-Content -Path $filePath -Raw
    $content = $content -replace '// @RestController', '@RestController'
    $content = $content -replace '// @RequestMapping', '@RequestMapping'
    Set-Content -Path $filePath -Value $content -NoNewline
    Write-Host "Processed $filePath"
}
