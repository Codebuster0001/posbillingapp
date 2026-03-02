import React, { useState, useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
    Building,
    Phone,
    Mail,
    Camera,
    Upload,
    Save,
    CheckCircle
} from 'lucide-react';
import { updateUser } from '../../features/auth/authSlice';
import api from '../../lib/api';

const Settings = () => {
    const { user } = useSelector((state) => state.auth);
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [successMessage, setSuccessMessage] = useState('');

    // State for Profile
    const [profileData, setProfileData] = useState({
        companyName: '',
        email: '',
        phoneNumber: '',
        countryId: '',
        stateId: '',
        cityId: '',
        logoUrl: ''
    });



    // States for Metadata
    const [countries, setCountries] = useState([]);
    const [states, setStates] = useState([]);
    const [cities, setCities] = useState([]);

    useEffect(() => {
        if (user) {
            fetchInitialData();
        }
        fetchCountries();
    }, [user]);

    useEffect(() => {
        if (profileData.countryId) fetchStates(profileData.countryId);
    }, [profileData.countryId]);

    useEffect(() => {
        if (profileData.stateId) fetchCities(profileData.stateId);
    }, [profileData.stateId]);

    const fetchInitialData = async () => {
        const companyId = user?.companyId || user?.CompanyId;
        console.log('Effective CompanyId for settings:', companyId);
        if (!companyId) {
            console.warn('No CompanyId found in user state:', user);
            return;
        }

        try {
            const [profileRes] = await Promise.all([
                api.get(`/api/Settings/profile/${companyId}`)
            ]);

            if (profileRes.ok && profileRes.data) {
                const d = profileRes.data;
                setProfileData({
                    companyName: d.companyName || d.CompanyName || '',
                    email: d.email || d.Email || '',
                    phoneNumber: d.phoneNumber || d.PhoneNumber || '',
                    countryId: d.countryId || d.CountryId || '',
                    stateId: d.stateId || d.StateId || '',
                    cityId: d.cityId || d.CityId || '',
                    logoUrl: d.logoUrl || d.LogoUrl || ''
                });
            }
        } catch (error) {
            console.error('Error fetching settings:', error);
        } finally {
            setLoading(false);
        }
    };

    const fetchCountries = async () => {
        try {
            const res = await api.get('/api/Metadata/countries');
            if (res.ok && res.data) {
                setCountries(res.data.map(c => ({
                    id: c.id || c.Id,
                    countryName: c.countryName || c.CountryName
                })));
            }
        } catch (error) {
            console.error('Error fetching countries:', error);
        }
    };

    const fetchStates = async (countryId) => {
        try {
            const res = await api.get(`/api/Metadata/states/${countryId}`);
            if (res.ok && res.data) {
                setStates(res.data.map(s => ({
                    id: s.id || s.Id,
                    stateName: s.stateName || s.StateName
                })));
            }
        } catch (error) {
            console.error('Error fetching states:', error);
        }
    };

    const fetchCities = async (stateId) => {
        try {
            const res = await api.get(`/api/Metadata/cities/${stateId}`);
            if (res.ok && res.data) {
                setCities(res.data.map(c => ({
                    id: c.id || c.Id,
                    cityName: c.cityName || c.CityName
                })));
            }
        } catch (error) {
            console.error('Error fetching cities:', error);
        }
    };

    const handleProfileSubmit = async (e) => {
        e.preventDefault();
        const companyId = user?.companyId || user?.CompanyId;
        if (!companyId) return;

        setSaving(true);
        try {
            const res = await api.put(`/api/Settings/profile/${companyId}`, profileData);
            if (res.ok) {
                dispatch(updateUser({
                    companyName: profileData.companyName,
                    phoneNumber: profileData.phoneNumber
                }));
                showSuccess('Profile updated successfully!');
            }
        } catch (error) {
            console.error('Update failed:', error);
        } finally {
            setSaving(false);
        }
    };

    const handleLogoUpload = async (e) => {
        const companyId = user?.companyId || user?.CompanyId;
        if (!companyId) return;

        const file = e.target.files[0];
        if (!file) return;

        const formData = new FormData();
        formData.append('file', file);

        try {
            const res = await api.post(`/api/Settings/logo/${companyId}`, formData);
            if (res.ok) {
                setProfileData({ ...profileData, logoUrl: res.data.logoUrl });
                dispatch(updateUser({ companyLogo: `${api.defaults.baseURL}${res.data.logoUrl}` }));
                showSuccess('Logo updated successfully!');
            }
        } catch (error) {
            console.error('Logo upload failed:', error);
        }
    };



    const showSuccess = (msg) => {
        setSuccessMessage(msg);
        setTimeout(() => setSuccessMessage(''), 3000);
    };



    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
            </div>
        );
    }

    return (
        <div className="p-4 md:p-8 space-y-8 animate-in fade-in duration-500 min-h-screen bg-[#f8fafc]">
            {/* Header */}
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div>
                    <h1 className="text-4xl font-extrabold tracking-tight text-slate-900">Settings</h1>
                    <p className="text-slate-500 mt-2 text-lg">Manage your digital storefront and financial roots.</p>
                </div>

            </div>

            {/* Success Alert */}
            {successMessage && (
                <div className="fixed top-8 right-8 z-50 bg-white border border-emerald-100 text-emerald-600 px-6 py-4 rounded-2xl shadow-2xl shadow-emerald-100 flex items-center gap-4 animate-in slide-in-from-right duration-500">
                    <div className="w-10 h-10 rounded-full bg-emerald-50 flex items-center justify-center">
                        <CheckCircle className="w-6 h-6" />
                    </div>
                    <span className="font-bold text-lg">{successMessage}</span>
                </div>
            )}

            <div className="flex flex-col md:flex-row gap-8">
                {/* Sidebar Tabs */}
                <div className="w-full md:w-72 space-y-2">
                    <button
                        className="w-full flex items-center gap-4 px-6 py-4 rounded-2xl text-base font-bold bg-white text-indigo-600 shadow-[0_20px_50px_rgba(79,70,229,0.1)] border border-indigo-50"
                    >
                        <div className="p-2 rounded-xl bg-indigo-50">
                            <Building className="w-5 h-5" />
                        </div>
                        Company Profile
                    </button>
                </div>

                {/* Content Area */}
                <div className="flex-1">
                    <div className="bg-white border border-slate-100 rounded-[2.5rem] p-8 md:p-12 shadow-sm">
                        <form onSubmit={handleProfileSubmit} className="space-y-10">
                            <div className="flex flex-col sm:flex-row items-center gap-10 pb-10 border-b border-slate-50">
                                <div className="relative group">
                                    <div className="w-32 h-32 rounded-[2rem] bg-slate-50 flex items-center justify-center overflow-hidden border-4 border-white shadow-xl group-hover:scale-105 transition-transform duration-500">
                                        {profileData.logoUrl ? (
                                            <img src={`${api.defaults.baseURL}${profileData.logoUrl}`} alt="Logo" className="w-full h-full object-cover" />
                                        ) : (
                                            <Building className="w-12 h-12 text-slate-300" />
                                        )}
                                    </div>
                                    <label className="absolute -bottom-2 -right-2 bg-indigo-600 text-white p-3 rounded-2xl shadow-xl cursor-pointer hover:bg-indigo-700 hover:rotate-12 transition-all">
                                        <Camera className="w-5 h-5" />
                                        <input type="file" className="hidden" accept="image/*" onChange={handleLogoUpload} />
                                    </label>
                                </div>
                                <div className="text-center sm:text-left space-y-2">
                                    <h3 className="text-2xl font-black text-slate-900 tracking-tight">Business Identity</h3>
                                    <p className="text-slate-500 max-w-md">Your logo and company name will appear on all digital receipts.</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider ml-1">Company Name</label>
                                    <div className="relative group">
                                        <Building className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-indigo-500 transition-colors" />
                                        <input
                                            type="text"
                                            value={profileData.companyName}
                                            onChange={(e) => setProfileData({ ...profileData, companyName: e.target.value })}
                                            className="w-full pl-12 pr-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all font-medium"
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider ml-1">Phone Number</label>
                                    <div className="relative group">
                                        <Phone className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-indigo-500 transition-colors" />
                                        <input
                                            type="tel"
                                            value={profileData.phoneNumber}
                                            onChange={(e) => setProfileData({ ...profileData, phoneNumber: e.target.value })}
                                            className="w-full pl-12 pr-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all font-medium"
                                            required
                                        />
                                    </div>
                                </div>
                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider ml-1">Email Address</label>
                                    <div className="relative group">
                                        <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-indigo-500 transition-colors" />
                                        <input
                                            type="email"
                                            value={profileData.email}
                                            onChange={(e) => setProfileData({ ...profileData, email: e.target.value })}
                                            className="w-full pl-12 pr-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all font-medium"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider">Country</label>
                                    <select
                                        value={profileData.countryId}
                                        onChange={(e) => setProfileData({ ...profileData, countryId: e.target.value, stateId: '', cityId: '' })}
                                        className="w-full px-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all"
                                    >
                                        <option value="">Select Country</option>
                                        {countries.map(c => <option key={c.id} value={c.id}>{c.countryName}</option>)}
                                    </select>
                                </div>
                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider">State</label>
                                    <select
                                        value={profileData.stateId}
                                        onChange={(e) => setProfileData({ ...profileData, stateId: e.target.value, cityId: '' })}
                                        className="w-full px-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all"
                                    >
                                        <option value="">Select State</option>
                                        {states.map(s => <option key={s.id} value={s.id}>{s.stateName}</option>)}
                                    </select>
                                </div>
                                <div className="space-y-3">
                                    <label className="text-sm font-black text-slate-700 uppercase tracking-wider">City</label>
                                    <select
                                        value={profileData.cityId}
                                        onChange={(e) => setProfileData({ ...profileData, cityId: e.target.value })}
                                        className="w-full px-6 py-4 bg-slate-50/50 border border-slate-200 rounded-[1.25rem] focus:border-indigo-500 outline-none transition-all"
                                    >
                                        <option value="">Select City</option>
                                        {cities.map(c => <option key={c.id} value={c.id}>{c.cityName}</option>)}
                                    </select>
                                </div>
                            </div>
                            <button
                                type="submit"
                                disabled={saving}
                                className="flex items-center gap-3 px-10 py-4 bg-slate-900 text-white rounded-[1.25rem] font-bold shadow-2xl hover:bg-slate-800 transition-all disabled:opacity-50"
                            >
                                {saving ? <Upload className="w-5 h-5 animate-spin" /> : <Save className="w-5 h-5" />}
                                Save Profile
                            </button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Settings;
