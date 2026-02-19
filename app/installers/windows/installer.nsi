!define APPNAME "IRSE | Bereia Versículos"
!define COMPANYNAME "IRSE"
!define DESCRIPTION "Um leitor de versículos bíblicos simples e elegante"
!define VERSIONMAJOR 1
!define VERSIONMINOR 0
!define VERSIONBUILD 0
!define INSTALLSIZE 164000

; Se BUILD_DIR não for definido externamente, define um padrão (para testes locais)
!ifndef BUILD_DIR
  !define BUILD_DIR "..\..\dist\windows"
!endif

Name "${APPNAME}"
; Salva o instalador na pasta dist
OutFile "${BUILD_DIR}\..\BereiaVerse_Setup_1.0.0.exe"
InstallDir "$PROGRAMFILES64\BereiaVerse"
InstallDirRegKey HKLM "Software\${APPNAME}" "Install_Dir"

RequestExecutionLevel admin

Page directory
Page instfiles

Section "Install"
    SetOutPath $INSTDIR
    
    ; --- Arquivos Principais ---
    File "${BUILD_DIR}\BereiaVerse.exe"
    File "${BUILD_DIR}\*.jar"
    File "${BUILD_DIR}\launch4j-config.xml"
    
    ; --- JRE Embutido ---
    SetOutPath "$INSTDIR\jre"
    File /r "${BUILD_DIR}\jre\*"
    
    SetOutPath $INSTDIR
    
    ; --- Atalhos ---
    CreateDirectory "$SMPROGRAMS\${APPNAME}"
    CreateShortCut "$SMPROGRAMS\${APPNAME}\${APPNAME}.lnk" "$INSTDIR\BereiaVerse.exe" "" "$INSTDIR\BereiaVerse.exe" 0
    CreateShortCut "$DESKTOP\${APPNAME}.lnk" "$INSTDIR\BereiaVerse.exe" "" "$INSTDIR\BereiaVerse.exe" 0
    
    ; --- Desinstalador ---
    WriteUninstaller "$INSTDIR\uninstall.exe"
    
    ; --- Registro ---
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName" "${APPNAME}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayIcon" "$INSTDIR\BereiaVerse.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "Publisher" "${COMPANYNAME}"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "NoRepair" 1
SectionEnd

Section "Uninstall"
    RMDir /r "$INSTDIR\jre"
    Delete "$INSTDIR\*.jar"
    Delete "$INSTDIR\*.exe"
    Delete "$INSTDIR\*.xml"
    RMDir "$INSTDIR"

    RMDir /r "$SMPROGRAMS\${APPNAME}"
    Delete "$DESKTOP\${APPNAME}.lnk"

    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
SectionEnd
