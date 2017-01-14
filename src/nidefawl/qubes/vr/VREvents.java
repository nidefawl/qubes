package nidefawl.qubes.vr;

public class VREvents {
    public static final int None = 0;
    public static final int TrackedDeviceActivated = 100;
    public static final int TrackedDeviceDeactivated = 101;
    public static final int TrackedDeviceUpdated = 102;
    public static final int TrackedDeviceUserInteractionStarted = 103;
    public static final int TrackedDeviceUserInteractionEnded = 104;
    public static final int IpdChanged = 105;
    public static final int EnterStandbyMode = 106;
    public static final int LeaveStandbyMode = 107;
    public static final int TrackedDeviceRoleChanged = 108;
    public static final int WatchdogWakeUpRequested = 109;
    public static final int ButtonPress = 200;
    public static final int ButtonUnpress = 201;
    public static final int ButtonTouch = 202;
    public static final int ButtonUntouch = 203;
    public static final int MouseMove = 300;
    public static final int MouseButtonDown = 301;
    public static final int MouseButtonUp = 302;
    public static final int FocusEnter = 303;
    public static final int FocusLeave = 304;
    public static final int Scroll = 305;
    public static final int TouchPadMove = 306;
    public static final int OverlayFocusChanged = 307;
    public static final int InputFocusCaptured = 400;
    public static final int InputFocusReleased = 401;
    public static final int SceneFocusLost = 402;
    public static final int SceneFocusGained = 403;
    public static final int SceneApplicationChanged = 404;
    public static final int SceneFocusChanged = 405;
    public static final int InputFocusChanged = 406;
    public static final int SceneApplicationSecondaryRenderingStarted = 407;
    public static final int HideRenderModels = 410;
    public static final int ShowRenderModels = 411;
    public static final int OverlayShown = 500;
    public static final int OverlayHidden = 501;
    public static final int DashboardActivated = 502;
    public static final int DashboardDeactivated = 503;
    public static final int DashboardThumbSelected = 504;
    public static final int DashboardRequested = 505;
    public static final int ResetDashboard = 506;
    public static final int RenderToast = 507;
    public static final int ImageLoaded = 508;
    public static final int ShowKeyboard = 509;
    public static final int HideKeyboard = 510;
    public static final int OverlayGamepadFocusGained = 511;
    public static final int OverlayGamepadFocusLost = 512;
    public static final int OverlaySharedTextureChanged = 513;
    public static final int DashboardGuideButtonDown = 514;
    public static final int DashboardGuideButtonUp = 515;
    public static final int ScreenshotTriggered = 516;
    public static final int ImageFailed = 517;
    public static final int RequestScreenshot = 520;
    public static final int ScreenshotTaken = 521;
    public static final int ScreenshotFailed = 522;
    public static final int SubmitScreenshotToDashboard = 523;
    public static final int ScreenshotProgressToDashboard = 524;
    public static final int Notification_Shown = 600;
    public static final int Notification_Hidden = 601;
    public static final int Notification_BeginInteraction = 602;
    public static final int Notification_Destroyed = 603;
    public static final int Quit = 700;
    public static final int ProcessQuit = 701;
    public static final int QuitAborted_UserPrompt = 702;
    public static final int QuitAcknowledged = 703;
    public static final int DriverRequestedQuit = 704;
    public static final int ChaperoneDataHasChanged = 800;
    public static final int ChaperoneUniverseHasChanged = 801;
    public static final int ChaperoneTempDataHasChanged = 802;
    public static final int ChaperoneSettingsHaveChanged = 803;
    public static final int SeatedZeroPoseReset = 804;
    public static final int AudioSettingsHaveChanged = 820;
    public static final int BackgroundSettingHasChanged = 850;
    public static final int CameraSettingsHaveChanged = 851;
    public static final int ReprojectionSettingHasChanged = 852;
    public static final int ModelSkinSettingsHaveChanged = 853;
    public static final int EnvironmentSettingsHaveChanged = 854;
    public static final int StatusUpdate = 900;
    public static final int MCImageUpdated = 1000;
    public static final int FirmwareUpdateStarted = 1100;
    public static final int FirmwareUpdateFinished = 1101;
    public static final int KeyboardClosed = 1200;
    public static final int KeyboardCharInput = 1201;
    public static final int KeyboardDone = 1202;
    public static final int ApplicationTransitionStarted = 1300;
    public static final int ApplicationTransitionAborted = 1301;
    public static final int ApplicationTransitionNewAppStarted = 1302;
    public static final int ApplicationListUpdated = 1303;
    public static final int ApplicationMimeTypeLoad = 1304;
    public static final int Compositor_MirrorWindowShown = 1400;
    public static final int Compositor_MirrorWindowHidden = 1401;
    public static final int Compositor_ChaperoneBoundsShown = 1410;
    public static final int Compositor_ChaperoneBoundsHidden = 1411;
    public static final int TrackedCamera_StartVideoStream = 1500;
    public static final int TrackedCamera_StopVideoStream = 1501;
    public static final int TrackedCamera_PauseVideoStream = 1502;
    public static final int TrackedCamera_ResumeVideoStream = 1503;
    public static final int PerformanceTest_EnableCapture = 1600;
    public static final int PerformanceTest_DisableCapture = 1601;
    public static final int PerformanceTest_FidelityLevel = 1602;
    public static final int VendorSpecific_Reserved_Start = 10000;
    public static final int VendorSpecific_Reserved_End = 19999;

    public static String evtToName(int evt) {
        switch (evt)
        {
            case 0:
                return "None";
                case 100:
                return "TrackedDeviceActivated";
                case 101:
                return "TrackedDeviceDeactivated";
                case 102:
                return "TrackedDeviceUpdated";
                case 103:
                return "TrackedDeviceUserInteractionStarted";
                case 104:
                return "TrackedDeviceUserInteractionEnded";
                case 105:
                return "IpdChanged";
                case 106:
                return "EnterStandbyMode";
                case 107:
                return "LeaveStandbyMode";
                case 108:
                return "TrackedDeviceRoleChanged";
                case 109:
                return "WatchdogWakeUpRequested";
                case 200:
                return "ButtonPress";
                case 201:
                return "ButtonUnpress";
                case 202:
                return "ButtonTouch";
                case 203:
                return "ButtonUntouch";
                case 300:
                return "MouseMove";
                case 301:
                return "MouseButtonDown";
                case 302:
                return "MouseButtonUp";
                case 303:
                return "FocusEnter";
                case 304:
                return "FocusLeave";
                case 305:
                return "Scroll";
                case 306:
                return "TouchPadMove";
                case 307:
                return "OverlayFocusChanged";
                case 400:
                return "InputFocusCaptured";
                case 401:
                return "InputFocusReleased";
                case 402:
                return "SceneFocusLost";
                case 403:
                return "SceneFocusGained";
                case 404:
                return "SceneApplicationChanged";
                case 405:
                return "SceneFocusChanged";
                case 406:
                return "InputFocusChanged";
                case 407:
                return "SceneApplicationSecondaryRenderingStarted";
                case 410:
                return "HideRenderModels";
                case 411:
                return "ShowRenderModels";
                case 500:
                return "OverlayShown";
                case 501:
                return "OverlayHidden";
                case 502:
                return "DashboardActivated";
                case 503:
                return "DashboardDeactivated";
                case 504:
                return "DashboardThumbSelected";
                case 505:
                return "DashboardRequested";
                case 506:
                return "ResetDashboard";
                case 507:
                return "RenderToast";
                case 508:
                return "ImageLoaded";
                case 509:
                return "ShowKeyboard";
                case 510:
                return "HideKeyboard";
                case 511:
                return "OverlayGamepadFocusGained";
                case 512:
                return "OverlayGamepadFocusLost";
                case 513:
                return "OverlaySharedTextureChanged";
                case 514:
                return "DashboardGuideButtonDown";
                case 515:
                return "DashboardGuideButtonUp";
                case 516:
                return "ScreenshotTriggered";
                case 517:
                return "ImageFailed";
                case 520:
                return "RequestScreenshot";
                case 521:
                return "ScreenshotTaken";
                case 522:
                return "ScreenshotFailed";
                case 523:
                return "SubmitScreenshotToDashboard";
                case 524:
                return "ScreenshotProgressToDashboard";
                case 600:
                return "Notification_Shown";
                case 601:
                return "Notification_Hidden";
                case 602:
                return "Notification_BeginInteraction";
                case 603:
                return "Notification_Destroyed";
                case 700:
                return "Quit";
                case 701:
                return "ProcessQuit";
                case 702:
                return "QuitAborted_UserPrompt";
                case 703:
                return "QuitAcknowledged";
                case 704:
                return "DriverRequestedQuit";
                case 800:
                return "ChaperoneDataHasChanged";
                case 801:
                return "ChaperoneUniverseHasChanged";
                case 802:
                return "ChaperoneTempDataHasChanged";
                case 803:
                return "ChaperoneSettingsHaveChanged";
                case 804:
                return "SeatedZeroPoseReset";
                case 820:
                return "AudioSettingsHaveChanged";
                case 850:
                return "BackgroundSettingHasChanged";
                case 851:
                return "CameraSettingsHaveChanged";
                case 852:
                return "ReprojectionSettingHasChanged";
                case 853:
                return "ModelSkinSettingsHaveChanged";
                case 854:
                return "EnvironmentSettingsHaveChanged";
                case 900:
                return "StatusUpdate";
                case 1000:
                return "MCImageUpdated";
                case 1100:
                return "FirmwareUpdateStarted";
                case 1101:
                return "FirmwareUpdateFinished";
                case 1200:
                return "KeyboardClosed";
                case 1201:
                return "KeyboardCharInput";
                case 1202:
                return "KeyboardDone";
                case 1300:
                return "ApplicationTransitionStarted";
                case 1301:
                return "ApplicationTransitionAborted";
                case 1302:
                return "ApplicationTransitionNewAppStarted";
                case 1303:
                return "ApplicationListUpdated";
                case 1304:
                return "ApplicationMimeTypeLoad";
                case 1400:
                return "Compositor_MirrorWindowShown";
                case 1401:
                return "Compositor_MirrorWindowHidden";
                case 1410:
                return "Compositor_ChaperoneBoundsShown";
                case 1411:
                return "Compositor_ChaperoneBoundsHidden";
                case 1500:
                return "TrackedCamera_StartVideoStream";
                case 1501:
                return "TrackedCamera_StopVideoStream";
                case 1502:
                return "TrackedCamera_PauseVideoStream";
                case 1503:
                return "TrackedCamera_ResumeVideoStream";
                case 1600:
                return "PerformanceTest_EnableCapture";
                case 1601:
                return "PerformanceTest_DisableCapture";
                case 1602:
                return "PerformanceTest_FidelityLevel";
                case 10000:
                return "VendorSpecific_Reserved_Start";
                case 19999:
                return "VendorSpecific_Reserved_End";
        }
        return "Unknown_Event_"+evt;
    }
}
