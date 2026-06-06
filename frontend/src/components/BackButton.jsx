import React from 'react';
import { useNavigate } from 'react-router-dom';

export default function BackButton({ className = '' }) {
  const navigate = useNavigate();

  const goBack = () => {
    try {
      if (window.history.length > 1) {
        navigate(-1);
      } else {
        navigate('/');
      }
    } catch (e) {
      navigate('/');
    }
  };

  return (
    <button
      type="button"
      aria-label="Go Back"
      onClick={goBack}
      className={
        `inline-flex items-center gap-3 rounded-full h-11 px-4 mt-6 mb-6 bg-[rgba(255,255,255,0.06)] border border-[rgba(255,255,255,0.12)] text-white backdrop-blur-sm shadow-md transition-transform duration-200 ease-in-out hover:shadow-[0_8px_22px_rgba(245,158,11,0.18)] hover:-translate-y-[2px] active:translate-y-[1px] focus:outline-none focus:ring-2 focus:ring-amber-400 z-50 ${className}`
      }
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" className="h-5 w-5 text-amber-500">
        <path d="M15 18l-6-6 6-6" />
      </svg>
      <span className="text-white font-semibold text-base">← Back</span>
    </button>
  );
}
