'use client';

import Image from 'next/image';
import Link from 'next/link';
import { useEffect, useState } from 'react';
import { supabase } from '../../lib/supabase';

type Parent = { id: string; name: string | null; phone: string | null; email: string | null };
type Student = {
  id: string;
  admission_number: string | null;
  first_name: string;
  middle_name: string | null;
  last_name: string | null;
  portal_code: string | null;
};

export default function ParentPortal() {
  const [parent, setParent] = useState<Parent | null>(null);
  const [students, setStudents] = useState<Student[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    async function loadPortal() {
      if (!supabase) {
        setError('Parent portal is not configured. Please contact the school administration.');
        setLoading(false);
        return;
      }

      const { data: { user } } = await supabase.auth.getUser();
      if (!user) {
        window.location.href = '/parent-login';
        return;
      }

      const { data: parentData, error: parentError } = await supabase
        .from('parents')
        .select('id, name, phone, email')
        .eq('profile_id', user.id)
        .maybeSingle();

      if (parentError) {
        setError(parentError.message);
        setLoading(false);
        return;
      }

      if (!parentData) {
        setError('No parent profile is linked to this account. Please contact the school administration.');
        setLoading(false);
        return;
      }

      setParent(parentData);

      const { data: links, error: linksError } = await supabase
        .from('student_parents')
        .select('student_id')
        .eq('parent_id', parentData.id);

      if (linksError) {
        setError(linksError.message);
        setLoading(false);
        return;
      }

      const ids = (links ?? []).map((row) => row.student_id);
      if (ids.length === 0) {
        setStudents([]);
        setLoading(false);
        return;
      }

      const { data: studentData, error: studentError } = await supabase
        .from('students')
        .select('id, admission_number, first_name, middle_name, last_name, portal_code')
        .in('id', ids)
        .order('first_name');

      if (studentError) {
        setError(studentError.message);
      } else {
        setStudents(studentData ?? []);
      }

      setLoading(false);
    }

    loadPortal();
  }, []);

  async function signOut() {
    await supabase?.auth.signOut();
    window.location.href = '/parent-login';
  }

  if (loading) {
    return <main className="placeholder"><div className="box"><div className="eyebrow">PARENT PORTAL</div><h1>Loading your account…</h1><p>Securely retrieving your parent and learner information.</p></div></main>;
  }

  return (
    <main className="portal-dashboard">
      <div className="portal-header">
        <Link href="/"><Image src="/aic-cathedral-logo.svg" width={62} height={62} alt="AIC Cathedral logo" /></Link>
        <div>
          <div className="eyebrow">PARENT PORTAL</div>
          <h1>Welcome{parent?.name ? `, ${parent.name}` : ''}</h1>
        </div>
        <button className="logout" onClick={signOut}>LOG OUT</button>
      </div>

      {error && <div className="error portal-error">{error}</div>}

      {!error && parent && (
        <>
          <section className="portal-info-grid">
            <article className="portal-info-card">
              <span>Parent Phone Number</span>
              <strong>{parent.phone || 'Not provided'}</strong>
            </article>
            <article className="portal-info-card">
              <span>Parent Email</span>
              <strong>{parent.email || 'Not provided'}</strong>
            </article>
          </section>

          <section className="portal-students">
            <div className="heading"><span>LINKED LEARNERS</span><h2>Student Special Codes</h2><p>These permanent portal codes are retrieved directly from the school's student records.</p></div>
            {students.length === 0 ? (
              <div className="portal-empty">No learner is currently linked to this parent account.</div>
            ) : (
              <div className="student-cards">
                {students.map((student) => (
                  <article className="student-card" key={student.id}>
                    <div className="student-card-top">
                      <div>
                        <span className="student-label">LEARNER</span>
                        <h3>{[student.first_name, student.middle_name, student.last_name].filter(Boolean).join(' ')}</h3>
                      </div>
                      <div className="special-code">
                        <span>STUDENT SPECIAL CODE</span>
                        <strong>{student.portal_code || 'Not assigned'}</strong>
                      </div>
                    </div>
                    {student.admission_number && <p>Admission Number: <strong>{student.admission_number}</strong></p>}
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </main>
  );
}
