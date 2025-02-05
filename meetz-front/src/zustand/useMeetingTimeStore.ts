import { create } from 'zustand';

interface MeetingTimeState {
  selectedDate: string | null;
  selectedDuration: { value: string; label: string } | null;
  selectedTime: { value: string; label: string } | null;
  selectedBreak: { value: string; label: string } | null;
  setSelectedDate: (date: string | null) => void;
  setSelectedDuration: (duration: { value: string; label: string } | null) => void;
  setSelectedTime: (time: { value: string; label: string } | null) => void;
  setSelectedBreak: (breakTime: { value: string; label: string } | null) => void;
  resetTimeStore: () => void;
}

const useMeetingTimeStore = create<MeetingTimeState>((set) => ({
  selectedDate: null,
  selectedDuration: null,
  selectedTime: null,
  selectedBreak: null,
  setSelectedDate: (date) => set({ selectedDate: date }),
  setSelectedDuration: (duration) => set({ selectedDuration: duration }),
  setSelectedTime: (time) => set({ selectedTime: time }),
  setSelectedBreak: (breakTime) => set({ selectedBreak: breakTime }),
  resetTimeStore: () => set({
    selectedDate: null,
    selectedDuration: null,
    selectedTime: null,
    selectedBreak: null,
  }),
}));

export default useMeetingTimeStore;
