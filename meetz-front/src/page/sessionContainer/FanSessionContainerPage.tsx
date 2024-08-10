import { useEffect, useState } from "react";
import { useSessionStore } from "../../zustand/useSessionStore";
import { EventSourcePolyfill } from "event-source-polyfill";
import SessionSwitchPage from "./session/pages/SessionSwitchPage";
import fetchUserData from "../../lib/fetchUserData";
import FanSessionPage from "./session/pages/FanSessionPage";
import FanSettingPage from "./setting/pages/FanSettingPage";
import useSaveImage from "../../hooks/session/useSaveImage";
import SessionLoadingPage from "./session/pages/SessionLoadingPage";
import { useOpenvidu } from "../../hooks/session/useOpenvidu";
type SessionInfo = {
  timer: number;
  wait: number;
  starName: string;
  nextStarName: string;
  sessionId: string;
};
const FanSessionContainerPage = () => {
  const { wait, setWait, setTakePhoto } = useSessionStore();
  const [type, setType] = useState(0);
  const {
    settingDone,
    setGetSessionId,
    setTimer,
    setStartName,
    setNextStarName,
  } = useSessionStore();
  const { sendImage } = useSaveImage();

  //로딩될 때마다 SSE 연결 시도
  useEffect(() => {
    fetchSSE();
  }, []);
  useEffect(() => {
    if (!settingDone && type === 1) {
      alert("카메라 설정이 완료되어야 미팅 입장이 진행됩니다.");
    }
  }, [type]);
  //fetchSSE 연결
  const fetchSSE = () => {
    console.log("SSE 연결 시도");
    const { accessToken } = fetchUserData();
    const eventSource = new EventSourcePolyfill(
      `${import.meta.env.VITE_API_DEPLOYED_URL}/api/sessions/sse`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Accept: "text/event-stream",
        },
        heartbeatTimeout: 7200 * 1000,
      }
    );

    //SSE와 연결 되었을 때
    eventSource.onopen = () => {
      console.log("!SSE 연결 성공!");
    };

    //SSE에게 메시지를 전달 받았을 때
    eventSource.onmessage = async (e: any) => {
      const res = await e.data;
      const parseData = JSON.parse(res);
      console.log(parseData);
      setType(parseData.type);
      if (parseData.type === 1||2) {
        await moveNextSession(parseData);
      } else if (parseData.type === 3) {
        setTakePhoto(true);
      }
      //SSE에러 발생 시 SSE와 연결 종료
      eventSource.onerror = (e: any) => {
        eventSource.close();
        if (e.error) {
          console.error(e.error);
        }
        if (e.target.readyState === EventSourcePolyfill.CLOSED) {
          console.log("EventSourcePolyFill-CLOSED");
        }
      };
    };
  };

  const moveNextSession = async (parseData: any) => {
    const info: SessionInfo = {
      timer: parseData.timer,
      starName: parseData.currentStarName,
      nextStarName: parseData.nextStarName,
      sessionId: parseData.viduToken,
      wait: parseData.waitingNum
    };
    await setInfo(info);
  };
  const setInfo = async (info: SessionInfo) => {
    return new Promise<void>((resolve) => {
      setTimer(info.timer);
      setStartName(info.starName);
      setNextStarName(info.nextStarName);
      setGetSessionId(info.sessionId);
      setWait(info.wait);
      resolve();
    });
  };
  switch (type) {
    case 2:
      return <SessionLoadingPage />;

    case 4:
      sendImage();
      return <SessionSwitchPage />;

    case 1:
    case 3:
      if (settingDone) {
        return <FanSessionPage />;
      }
      break;

    default:
      return <FanSettingPage />;
  }
};

export default FanSessionContainerPage;
