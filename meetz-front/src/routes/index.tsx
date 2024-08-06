import React from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import LoginPage from '../page/login/LoginPage';
import SignupPage from '../page/signup/SignupPage';
import YetMeetingPage from '../page/meeting/page/YetMeetingPage';
import EndMeetingPage from '../page/meeting/page/EndMeetingPage';
import MeetingLayout from '../page/meeting/MeetingLayout';
import DetailPage from '../page/meeting/page/DetailPage';
import MonitorPage from '../page/meeting/page/MonitorPage';
import CreatePage from '../page/meeting/page/CreatePage';
import ModifyPage from '../page/meeting/page/ModifyPage';
import ChatPage from '../page/meeting/page/ChatPage';
import LoadChatPage from '../page/meeting/page/LoadChatPage';
import FanSettingPage from '../page/sessionContainer/setting/pages/FanSettingPage';
import RecordPage from '../page/RecordPage';
import SessionContainerPage from '../page/sessionContainer/SessionContainerPage';
import StarLoadingPage from '../page/sessionContainer/setting/pages/StarLoadingPage';
import BlacklistCheckPage from '../page/meeting/page/BlacklistCheckPage';

const Router: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path='/' element={<LoginPage />} />
        <Route path='/signup' element={<SignupPage />} />
        <Route path='/setting' element={<FanSettingPage />} />
        <Route path='/loading' element={<StarLoadingPage />} />
        <Route path='/manage/:meetingId' element={<ChatPage />} />
        <Route path='/meeting' element={<MeetingLayout />}>
          {/* 미팅 미완료 */}
          <Route path='load/:meetingId' element={<LoadChatPage />} />
          <Route path='yet' element={<YetMeetingPage />} />
          <Route path='create' element={<CreatePage />} />
          <Route path='modify/:meetingId' element={<ModifyPage />} />
          <Route path='detail/:meetingId' element={<DetailPage />} />
          {/* 블랙 리스트 */}
          <Route path='blacklist' element={<BlacklistCheckPage />} />
          {/* 미팅 완료 */}
          <Route path='end' element={<EndMeetingPage />} />
          <Route path='monitor/:meetingId' element={<MonitorPage />} />
        </Route>
        <Route path='/session' element={<SessionContainerPage />} />
        <Route path='/test' element={<RecordPage />} />
      </Routes>
    </BrowserRouter>
  );
};

export default Router;
