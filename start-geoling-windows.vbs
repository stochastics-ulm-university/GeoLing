Dim objShell
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

' start application
objShell.Run("javaw " & MAX_HEAP & " -splash:geoling-splash-screen.png -jar geoling.jar")

Set objShell = Nothing
