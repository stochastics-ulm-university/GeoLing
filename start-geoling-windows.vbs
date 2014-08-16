Set objShell = WScript.CreateObject("WScript.Shell")

' set working directory
Set objFSO = CreateObject("Scripting.FileSystemObject")
Set objFile = objFSO.GetFile(Wscript.ScriptFullName)
objShell.CurrentDirectory = objFSO.GetParentFolderName(objFile)

' options for Java virtual machine:
' set maximum heap size to allow GeoLing to use more memory:
' - default max heap size:
'   (empty value, i.e., no parameter, means that Java auto-selects a value
'    depending on your total physical memory installed, usually 1/4)
'Const MAX_HEAP = ""
' - user-defined max heap size:
'   (note that at most ~1400m to ~1600m is possible on 32-bit machines)
Const MAX_HEAP = "-Xmx1400m"

' detect Java: try to find Java installed at various locations
' (preference for 64-bit Java and version 8 instead of 7)
Const JAVAW_EXE = "javaw.exe"
Const JAVAW_7_PATH = "\Java\jre7\bin\javaw.exe"
Const JAVAW_8_PATH = "\Java\jre8\bin\javaw.exe"
Set fileSystemObject = CreateObject("Scripting.FileSystemObject")
javawPath = objShell.ExpandEnvironmentStrings("%PROGRAMFILES%" & JAVAW_8_PATH)
If Not fileSystemObject.FileExists(javawPath) Then
  javawPath = objShell.ExpandEnvironmentStrings("%PROGRAMFILES%" & JAVAW_7_PATH)
End If
' now explicitly try 32-bit Java
If Not fileSystemObject.FileExists(javawPath) Then
  javawPath = objShell.ExpandEnvironmentStrings("%PROGRAMFILES(x86)%" & JAVAW_8_PATH)
End If
If Not fileSystemObject.FileExists(javawPath) Then
  javawPath = objShell.ExpandEnvironmentStrings("%PROGRAMFILES(x86)%" & JAVAW_7_PATH)
End If
' fallback to javaw.exe without path
If Not fileSystemObject.FileExists(javawPath) Then
  javawPath = JAVAW_EXE
End If

' start application
objShell.Run("""" & javawPath & """ " & MAX_HEAP & " -splash:geoling-splash-screen.png -jar geoling.jar")

Set objShell = Nothing
